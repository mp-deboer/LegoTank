define ANNOUNCE_BODY
usage: make [option] [-n]
Available options:
- default (no option given): Compiles class files only
- start:    Compiles class files and starts via class files
- all:      Runs 'clean', then bulk compiles class files
- allstart: Runs 'all' and 'start'
- jar:      Compiles class files and creates jar file
- jarstart: Runs 'jar', then starts via jar
- alljar:   Runs 'rjar', 'all' and then 'jar'
- clean:    Removes class file directory
- rjar:     Remove jar file (used by option 'alljar')
- deploy:   Copy jar file to home directory

Optional: Add argument -n to 'dry-run' (show commands, but do not execute them).
endef
export ANNOUNCE_BODY

CC = javac
RC = java
RFLAGS = -ea -Djava.library.path=".:/usr/lib/jni"
CLASSPATH = /usr/share/java/jinput.jar:./bin
SOURCES = $(wildcard src/tankpack/*.java)
CLASSES = $(SOURCES:src/tankpack/%.java=bin/tankpack/%.class)
JAR = Tank.jar

# Default target: incremental compilation
default: $(CLASSES)

# Incremental rule: only recompile the changed .java file
bin/tankpack/%.class: src/tankpack/%.java
	$(CC) -classpath $(CLASSPATH) -d bin $<

# Other options
start: $(CLASSES)
	$(RC) $(RFLAGS) -classpath $(CLASSPATH):. tankpack.Main

# Bulk compile class files, even with unchanged java files
all: clean
	$(CC) -classpath $(CLASSPATH) -d bin $(SOURCES)

allstart: all start

jar: $(JAR)

$(JAR): $(CLASSES)
	cd bin && jar cfem ../$@ tankpack/Main ../Manifest.txt tankpack -C .. sound

jarstart: jar
	$(RC) $(RFLAGS) -jar $(JAR)

alljar: rjar all jar

clean:
	rm -rf bin/tankpack

rjar:
	rm -f $(JAR)

deploy: jar
	cp $(JAR) /home/pi

help:
	@echo "$$ANNOUNCE_BODY"