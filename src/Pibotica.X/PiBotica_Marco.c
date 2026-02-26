/*
 * File:   PiBotica_Marco.c
 * Author: Marco de Boer
 *
 * Created on 21 june 2014, 11:16
 */

#include <xc.h>                                    //PIC hardware mapping
//#define _XTAL_FREQ 500000                          //Used by the XC8 delay_ms(x) macro
#define _XTAL_FREQ 16000000

//config bits that are part-specific for the PIC16F1829
#pragma config FOSC=INTOSC, WDTE=OFF, PWRTE=OFF, MCLRE=ON, CP=OFF, CPD=OFF, BOREN=ON, CLKOUTEN=OFF, IESO=OFF, FCMEN=OFF
#pragma config WRT=OFF, PLLEN=OFF, STVREN=OFF, LVP=OFF

//bool
#define bool unsigned char
#define false 0
#define true !false

//ADC defines
#define ADCIR1 0b00000001 //AN0  - RA0
#define ADCIR2 0b00000101 //AN1  - RA1
#define ADCIR3 0b00001101 //AN3  - RA4
#define ADCIR4 0b00011001 //AN6  - RC2
#define ADCIR5 0b00101001 //AN10 - RB4

//SPI defines
#define SPIBUFFER SSP2BUFbits.SSPBUF
#define DATA (spiData & 0x1F)
#define MULT 6 // Multiplier on data, motor speed = DATA * MULT + BOOST, where data is max 31
#define BOOST 69 //Starting PWM at 69/255 (at this point the motors start turning)

//Soft PWM defines and global variable
#define TOTAL 65536
#define BASEFREQ 1333
#define DUTYPERIOD 26
#define MAXDUTIES 31 // SPI data can only be max 31
#define DUTYBOOST 527 //(BASEFREQ - (DUTYPERIOD * MAXDUTIES))
unsigned int PWMdutyWidth;
unsigned int PWMtimeBetweenDuties;
bool duty; // Is it duty time?
bool left; // Direction of the turret, when true, go left, when false, go right

//adc values
unsigned char adcIR1;
unsigned char adcIR2;
unsigned char adcIR3;
unsigned char adcIR4;
unsigned char adcIR5;

void picinit();
void configure_PORTS();
void configure_PWM();
void configure_Soft_PWM();
void configure_SPI();
unsigned char read_ADC(unsigned char adcCon);
void execute_SPIcmd(unsigned char spiData);
void setTurret(unsigned char dutyAmount);

void main()
{
    picinit();

    while (1) //read adc in main loop
    {
        adcIR1 = read_ADC(ADCIR1);
        adcIR2 = read_ADC(ADCIR2);
        adcIR3 = read_ADC(ADCIR3);

        // When having sensors on IR4 and IR5 (they must be set as analog input for that):
        if (TRISCbits.TRISC2 && ANSELCbits.ANSC2)
        {
            adcIR4 = read_ADC(ADCIR4);
        }
        if (TRISBbits.TRISB4 && ANSELBbits.ANSB4)
        {
            adcIR5 = read_ADC(ADCIR5);
        }
    }

}

void interrupt ISR(void)
{
    // When SPI buffer has fully received a new byte
    if (PIR4bits.SSP2IF)
    {
        // verify buffer is full
        if (SSP2STATbits.BF)
        {
            execute_SPIcmd(SPIBUFFER);
        }

        if (SSP2CON1bits.SSPOV)
        {
            // Receive overflow, must be cleared

            // Datasheet p. 289:
            //    "A new byte is received while the SSPxBUF register is still holding the previous data. In case of overflow, the data in SSPxSR is lost.
            //    Overflow can only occur in Slave mode. In Slave mode, the user must read the SSPxBUF, even if only transmitting data, to avoid
            //    setting overflow. In Master mode, the overflow bit is not set since each new reception (and transmission) is initiated by writing to the
            //    SSPxBUF register (must be cleared in software)."

            SSP2CON1bits.SSPOV = 0;
            LATBbits.LATB6 = PORTBbits.RB6 ^ 1; // toggle LED to indicate receive overflow
        }

        if (SSP2CON1bits.WCOL)
        {
            // Write collision, must be cleared

            // Datasheet p. 289:
            //    "The SSPxBUF register is written while it is still transmitting the previous word (must be cleared in software)"
            SSP2CON1bits.WCOL = 0;
            LATBbits.LATB6 = PORTBbits.RB6 ^ 1; // toggle LED to indicate write (transmit) collision
        }

        PIR4bits.SSP2IF = 0; // must clear in software
    }

    //Soft PWM based on Timer1
    if (PIR1bits.TMR1IF)
    {
        // When we just ended a duty-cycle
        if (duty)
        {
            //On second overflow (after duty-cycle):
            // set pin off
            LATCbits.LATC2 = 0;
            LATBbits.LATB4 = 0;

            // set timer until next BaseOverflow: TOTAL - BASEFREQ + (DUTYPERIOD * duties) + DUTYBOOST
            TMR1 = PWMtimeBetweenDuties;

            //No more duty-cycle
            duty = false;
        }
        else
        {
            //On timer overflow (base frequence):
            // set pin on
            if (left)
            {
                //turn left
                LATCbits.LATC2 = 1;
                LATBbits.LATB4 = 0;
            }
            else
            {
                //turn right
                LATCbits.LATC2 = 0;
                LATBbits.LATB4 = 1;
            }

            // Set duty-cycle: TOTAL - (DUTYPERIOD * duties) - DUTYBOOST
            TMR1 = PWMdutyWidth;

            //Duty cycle started
            duty = true;
        }

        //Datasheet p.191:
        //    "The interrupt is cleared by clearing the TMR1IF bit in the Interrupt Service Routine."
        PIR1bits.TMR1IF = 0;
    }
}

void picinit()
{
    //CLOCK (datasheet p. 70)
    //OSCCON = 0b00111000;    //500KHz Fosc
    OSCCON = 0b01111000; // 16Mhz Fosc

    // Global var init
    PWMdutyWidth = 0;
    PWMtimeBetweenDuties = 0;
    duty = false;
    left = true;
    adcIR1 = 0;
    adcIR2 = 0;
    adcIR3 = 0;
    adcIR4 = 0;
    adcIR5 = 0;

    // Configure ADC
    // ADCON1 = 0b00010000; //left justified - FOSC/8 speed - Vref is Vdd       //<- voor 500KHz
    ADCON1 = 0b01100000; //left justified - FOSC/64 speed - Vref is Vdd      //<- voor 16MHz

    configure_PORTS();
    configure_PWM();
    configure_Soft_PWM();
    configure_SPI();

    //test pwm:
    /*
    unsigned char tmpduty = 30;
    PWMdutyWidth = TOTAL - (DUTYPERIOD * tmpduty) - DUTYBOOST;
    PWMtimeBetweenDuties = TOTAL - BASEFREQ + (DUTYPERIOD * tmpduty) + DUTYBOOST;

    left = true;
    duty = false;

    //TMR1H:TMR1L
    TMR1 = PWMtimeBetweenDuties;

    //Clear Timer1 interupt
    PIR1bits.TMR1IF = 0;
    //Timer on
    T1CONbits.TMR1ON = 1;
     */
    //test pwm using pwm module
    /*
    PSTR2CON = 0b00011111;
    CCPR2L = 100;
    TRISCbits.TRISC3 = 1;
     */

    // Enable interrupts
    INTCONbits.PEIE = 1;
    INTCONbits.GIE = 1;
}

void configure_PORTS()
{
    // PIN init (u = unused, we initialise them as digital inputs)

    /*
     * IR1 - IR3 are inputs from the sensors
     * IR4 and IR5 are outputs to Turret
     * IN1 and IN2 are outputs to Motor1
     * IN3 and IN4 are outputs to Motor2
     * MISO, MOSI, CE and SCLK are SPI funtions
     */

    // A
    // Port                RA0   RA1   RA2   RA3   RA4   RA5   RA6   RA7
    // Function            IR1   IR2   IN3   BTN   IR3   -     N/A   N/A
    // TRIS (0=OUT, 1=IN)  1     1     0     1!    1     u=1
    // ANSEL (0=D, 1=A)    1     1     0     0     1     u=0
    TRISA = 0b00111011;
    ANSELA = 0b00010011;
    LATA = 0b00000000;

    // B
    // Port                RB0   RB1   RB2   RB3   RB4   RB5   RB6   RB7
    // Function            N/A   N/A   N/A   N/A   IR5   MOSI  LED1  SCLK
    // TRIS (0=OUT, 1=IN)                          0     1     0     1
    // ANSEL (0=D, 1=A)                            0     0     0     0

    TRISB = 0b10100000;
    ANSELB = 0b00000000;
    LATB = 0b01000000; // LED1 = 1, means turn it off!

    // C
    // Port                RC0   RC1   RC2   RC3   RC4   RC5   RC6   RC7
    // Function            CE    MISO  IR4   IN2   EN    IN1   IN4   -
    // TRIS (0=OUT, 1=IN)  1     0     0     0     0     0     0     u=1
    // ANSEL (0=D, 1=A)    0     0     0     0     0     0     0     u=0
    TRISC = 0b10000001;
    ANSELC = 0b00000000;
    LATC = 0b00010000; // Enable Motor 1 and 2 (EN = 1)
}

void configure_PWM()
{
    CCPTMRS = 0b00000000; // tmr2 for all pwm

    // Datasheet p. 236:
    // CCPxCON
    // Bit 7-6: PxM - Enhanced PWM Output Configuration bits
    //  00 = Single output; PxA modulated; PxB, PxC, PxD assigned as port pins

    // Bit 5-4: DCxB - PWM Duty Cycle Least Significant bits
    //  The 2 LSBs of the PWM duty cycle.
    //  The eight MSBs are found in CCPRxL

    // Bit <3:0>: CCPxM - ECCPx Mode Select bits
    //  11xx = PWM mode (CCP Modules only)
    CCP1CON = 0b00001100;
    CCP2CON = 0b00001100;
    CCP3CON = 0b00001100;
    CCP4CON = 0b00001100;

    /*Little bit unusefull info below. Basically it means, don't mess with the PSTR2CON register*/
    // Datasheet p. 122:
    //    CCP2 function is on RC3 by default.

    // It does require Steering on CCPx/PxA in the PSTR2CON register,
    //  as can be derived from Table 24-9 on p. 223:
    //  "Note 1: PWM Steering enables outputs in Single mode."
    // Luckely that is ON by default.

    // Datasheet p. 233:
    //    "In Single Output mode, PWM steering allows any of the
    //    PWM pins to be the modulated signal. Additionally, the
    //    same PWM signal can be simultaneously available on
    //    multiple pins.
    //    Once the Single Output mode is selected
    //    (CCPxM<3:2> = 11 and PxM<1:0> = 00 of the
    //    CCPxCON register), the user firmware can bring out
    //    the same PWM signal to one, two, three or four output
    //    pins by setting the appropriate STRx<D:A> bits of the
    //    PSTRxCON register"

    // Enable Steering / PWM ONLY on PxA (p. 240):
    // PSTR2CON = 0b00000001; // <- Default setting

    // When enabled on PxB or PxD, the Turret pin (RC2) will receive PWM from CCP2, which we don't want.
    /**/

    // 8 + 2 = 10 bit resolution, we won't use the 2 LSBs though
    PR2 = 255;

    // Clear the PWM duty cycles
    CCPR1H = 0;
    CCPR2H = 0;
    CCPR3H = 0;
    CCPR4H = 0;
    CCPR1L = 0;
    CCPR2L = 0;
    CCPR3L = 0;
    CCPR4L = 0;

    T2CONbits.TMR2ON = 1; // enable timer
}

void configure_Soft_PWM()
{
    //For Soft PWM we are going to use Timer1, using the internal clock source Fcy
    //Datasheet p.195:
    //  "TMR1CS<1:0>: Timer1 Clock Source Select bits
    //  00 = Timer1 clock source is instruction clock (FOSC/4)"
    T1CONbits.TMR1CS = 0b00;


    //We want a Soft PWM with a base frequency of 3000Hz = 1/3000 sec = 333.3333 us
    //on top of that we want a duty-cycle which can be set to 0-31 of the base frequency, therefor we need a minimum counter of 333.3333 / 31 = 10.7527 us

    //With Timer1, we have:
    //2^16 = 65536 possible cycles

    //333.33 us spread out over the cycles: 333.33 / 65536 = 0.0050862630208333 us per cycle
    //Foc = 16MHZ
    //Fcy = 16 / 4 = 4MHz
    //Fcy = 1 / 4000000 = 0.25us

    //Fcy : us per cycle = 1 : 0.0203450520833333
    //So, in order to get at least 333.3333 us spread out over the cycles, we need a prescaler of 1.

    //Datasheet p.195:
    //  "T1CKPS<1:0>: Timer1 Input Clock Prescale Select bits
    //  00 = 1:1 Prescale value"
    T1CONbits.T1CKPS = 0b00;


    //With that determined, we can calculate the values for timer1 in order to get 333.3333 us
    //  333.3333 / 0.25 = 1333.3333
    //  10.7527 / 0.25 = 43.01075268817204

    //Since we need an integer, we will round down to 43 increments, now we have to calculate back:
    //Duty-cycle (1/31) = 43*0.25us = 10.75us
    //Base-frequency (31) = 10.75*31=333.25us
    //
    //Base-frequency becomes: 1/333.25us ~= 3000.7502Hz
    //
    //Timer1 settings will become:
    //TOTAL = 65536
    //BASEFREQ = 1333
    //DUTYPERIOD = 43
    //duties = 0-31
    /* See 'Soft PWM defines' on top of this code */

    //Datasheet p. 188:
    //    "21.7 Timer1 Interrupt
    //    The Timer1 register pair (TMR1H:TMR1L) increments to FFFFh and rolls over to 0000h.
    //    When Timer1 rolls over, the Timer1 interrupt flag bit of the PIR1 register is set.
    //    To enable the interrupt on rollover, you must set these bits:
    //    - TMR1ON bit of the T1CON register
    T1CONbits.TMR1ON = 0; // We only enable the timer when Soft PWM is required

    //    - TMR1IE bit of the PIE1 register
    PIE1bits.TMR1IE = 1;

    //    - PEIE bit of the INTCON register
    //    - GIE bit of the INTCON register
    /* We enable these bits at the end of picinit() */

    //    Note: The TMR1H:TMR1L register pair and the TMR1IF bit should be cleared before enabling interrupts."
    TMR1H = 0;
    TMR1L = 0;
    PIR1bits.TMR1IF = 0;
}

void configure_SPI()
{
    // SPI
    //  sck2	in	clock	RB7
    //  sdix2	in	dataIn	RB5
    //  sdox2	out	dataOut	RC1         alt RA5
    //  ssx2	in	select	RC0         alt RA4

    // SPI config (info from datasheet p. 248, 288-289)
    //  While operated in SPI Slave mode the
    //  SMP bit of the SSPxSTAT register must
    //  remain clear.
    SSP2STATbits.SMP = 0;

    //  CKE = 1: Transmit occurs on transition from active to Idle clock
    SSP2STATbits.CKE = 1;

    // CKP (bit 4 in SSP2CON1) is set on 1, which means:
    //  Idle state for clock is a high level.

    //  The SSx pin allows a Synchronous Slave mode. The
    //  SPI must be in Slave mode with SSx pin control
    //  enabled: SSP2CON1<3:0> = 0100 state
    SSP2CON1 = 0b00100100;
    //  Note:
    //    When the SPI is used in Slave mode with
    //    CKE set; the user MUST enable SSx pin
    //    control.


    // SPI interrupt
    // SSP2IF    <-- SPI interrupt flag
    PIE4bits.SSP2IE = 1;

    // start SPI
    SSP2CON1bits.SSPEN = 1;
}

unsigned char read_ADC(unsigned char adcCon)
{
    ADCON0 = adcCon;
    __delay_us(200);
    GO = 1;
    while (GO) continue;
    return ADRESH;
}

void execute_SPIcmd(unsigned char spiData)
{
    switch (spiData >> 5 & 0x07) // CODE
    {
        case 0:
            // Commands
            switch (DATA) // DATA = (spiData & 0x1F)
            {
                case 1:
                    // return IR1 value
                    SPIBUFFER = adcIR1;
                    break;
                case 2:
                    // return IR2 value
                    SPIBUFFER = adcIR2;
                    break;
                case 3:
                    // return IR3 value
                    SPIBUFFER = adcIR3;
                    break;
                case 4:
                    // return IR4 value
                    SPIBUFFER = adcIR4;
                    break;
                case 5:
                    // return IR5 value
                    SPIBUFFER = adcIR5;
                    break;
                case 10:
                    // Turn LED1 on
                    LATBbits.LATB6 = 0;
                    break;
                case 11:
                    // Turn LED1 off
                    LATBbits.LATB6 = 1;
                    break;
                case 12:
                    // Toggle LED1
                    LATBbits.LATB6 = LATBbits.LATB6^1;
                    break;
                default:
                    __delay_us(1);
                    break;
            }
            break;

        case 1:
            // Setup commands
            switch (DATA)
            {
                case 31:
                    // Reset everything
                    // Motor1
                    CCPR1L = 0;
                    CCPR2L = 0;
                    // Motor2
                    CCPR3L = 0;
                    CCPR4L = 0;
                    // Turret
                    LATBbits.LATB4 = 0;
                    LATCbits.LATC2 = 0;
                    // LED
                    LATBbits.LATB6 = 1;
                    break;
                default:
                    __delay_us(1);
                    break;
            }
            break;

        case 2:
            // Turret right
            left = false;
            setTurret(DATA);
            break;

        case 3:
            // Turret left
            left = true;
            setTurret(DATA);
            break;

        case 4:
            // Motor 1 forward
            CCPR2L = 0;
            if (DATA == 0)
                CCPR1L = 0;
            else
                CCPR1L = (DATA * MULT + BOOST);
            break;

        case 5:
            // Motor 1 backward
            CCPR1L = 0;
            if (DATA == 0)
                CCPR2L = 0;
            else
                CCPR2L = (DATA * MULT + BOOST);
            break;

        case 6:
            // Motor 2 forward
            CCPR4L = 0;
            if (DATA == 0)
                CCPR3L = 0;
            else
                CCPR3L = (DATA * MULT + BOOST);
            break;

        case 7:
            // Motor 2 backward
            CCPR3L = 0;
            if (DATA == 0)
                CCPR4L = 0;
            else
                CCPR4L = (DATA * MULT + BOOST);
            break;
    }
}

void setTurret(unsigned char dutyAmount)
{
    if (dutyAmount == 0)
    {
        //duties(0) = pin off, timer off
        T1CONbits.TMR1ON = 0;
        //Clear Timer1 interupt
        PIR1bits.TMR1IF = 0;

        LATCbits.LATC2 = 0;
        LATBbits.LATB4 = 0;
    }
    else if (dutyAmount == MAXDUTIES)
    {
        //duties(31) = pin on, timer off
        T1CONbits.TMR1ON = 0;
        //Clear Timer1 interupt
        PIR1bits.TMR1IF = 0;

        if (left)
        {
            LATCbits.LATC2 = 1;
            LATBbits.LATB4 = 0;
        }
        else
        {
            LATCbits.LATC2 = 0;
            LATBbits.LATB4 = 1;
        }
    }
    else
    {
        //duties(1-30)
        PWMdutyWidth = TOTAL - (DUTYPERIOD * dutyAmount) - DUTYBOOST;
        PWMtimeBetweenDuties = TOTAL - BASEFREQ + (DUTYPERIOD * dutyAmount) + DUTYBOOST;

        // if timer was off, reset timer:
        if (!T1CONbits.TMR1ON)
        {
            duty = false;

            //TMR1H:TMR1L
            TMR1 = PWMtimeBetweenDuties;

            //Clear Timer1 interupt
            PIR1bits.TMR1IF = 0;

            //Timer on
            T1CONbits.TMR1ON = 1;
        }
    }
}