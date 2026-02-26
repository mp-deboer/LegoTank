CC = javac
CFLAGS = -classpath /opt/pi4j/lib/pi4j-core.jar:/usr/share/java/jinput.jar:./bin -sourcepath ./src/tankpack
classes = bin/tankpack/Main.class

RC = java
RFLAGS = -ea -Djava.library.path=".:/usr/lib/jni" -cp Tank.jar

.SUFFIXES: .java .class .jar

bin/tankpack/%.class: src/tankpack/%.java
	$(CC) $(CFLAGS) -d ./bin $<

# Requires having CommunicationDriver.class
# Temporarily remove StateMachine depencencies from CommunicationDriver if CommunicationDriver.class does not exist yet
all: clean Tank.jar

Tank.jar: $(classes)
	cd bin; jar cfem $@ Main ../Manifest.txt tankpack -C .. sound;mv $@ ..

start: Tank.jar
	sudo $(RC) $(RFLAGS) tankpack.Main

clean: rjar
	find bin/tankpack -name '*.class' ! -name 'CommunicationDriver.class' -exec $(RM) {} \;
	
rjar:
	$(RM) Tank.jar
	
bin/tankpack/Main.class: src/tankpack/Main.java bin/tankpack/CommunicationDriver.class bin/tankpack/HardwareDriver.class bin/tankpack/Mode.class bin/tankpack/PsComponent.class bin/tankpack/Buttons.class bin/tankpack/Led2.class bin/tankpack/PsController.class bin/tankpack/Turret.class bin/tankpack/Motors.class bin/tankpack/Tracks.class bin/tankpack/Sounds.class bin/tankpack/SensorPosition.class bin/tankpack/LineFollower.class

bin/tankpack/StateMachine.class: src/tankpack/StateMachine.java bin/tankpack/CommunicationDriver.class

bin/tankpack/Mode.class: src/tankpack/Mode.java bin/tankpack/StateMachine.class

bin/tankpack/HardwareDriver.class: src/tankpack/HardwareDriver.java bin/tankpack/StateMachine.class bin/tankpack/SensorPosition.class

bin/tankpack/PsController.class: src/tankpack/PsController.java bin/tankpack/StateMachine.class

bin/tankpack/Buttons.class: src/tankpack/Buttons.java bin/tankpack/StateMachine.class bin/tankpack/PsController.class bin/tankpack/PsComponent.class

bin/tankpack/Turret.class: src/tankpack/Turret.java bin/tankpack/StateMachine.class bin/tankpack/PsController.class bin/tankpack/PsComponent.class

bin/tankpack/Tracks.class: src/tankpack/Tracks.java bin/tankpack/StateMachine.class bin/tankpack/PsController.class bin/tankpack/PsComponent.class

bin/tankpack/Led2.class: src/tankpack/Led2.java bin/tankpack/StateMachine.class

bin/tankpack/Motors.class: src/tankpack/Motors.java bin/tankpack/StateMachine.class

bin/tankpack/Sounds.class: src/tankpack/Sounds.java bin/tankpack/StateMachine.class

bin/tankpack/LineFollower.class: src/tankpack/LineFollower.java bin/tankpack/SensorPosition.class bin/tankpack/StateMachine.class
