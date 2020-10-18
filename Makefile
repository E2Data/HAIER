##
## ckatsak, Sun Oct 18 22:41:36 EEST 2020
##
MVN ?= /opt/apache-maven-3.6.3/bin/mvn
JAVA_HOME ?= /opt/ckatsak/jdk-jvmci/graal-jvmci-8/openjdk1.8.0_265/linux-amd64/product

TARGETS = run run-forked fatjar dep-tree dep-anal clean
.PHONY: help $(TARGETS)

help:  ### Makefile-help-message
	@grep -E '^[a-zA-Z0-9_-]+:.*?### .*$$' $(MAKEFILE_LIST) \
	| sed -n 's/^\(.*\): \(.*\)###\(.*\)/\1\3/p' \
	| column -t  -s ' '
	@echo
	@echo "JAVA_HOME =" $(JAVA_HOME)
	@echo "MVN =" $(MVN)

run:  ### jetty-mvn-plugin
	@echo $(JAVA_HOME)
	$(MVN) clean jetty:run

run-forked:  ### jetty-mvn-plugin-fork
	@echo $(JAVA_HOME)
	$(MVN) clean jetty:run-forked

fatjar:  ### fat-executable-jar
	@echo $(JAVA_HOME)
	$(MVN) clean package assembly:single

dep-tree:  ### dependency-tree
	@echo $(JAVA_HOME)
	$(MVN) dependency:tree

dep-anal:  ### dependency-analysis
	@echo $(JAVA_HOME)
	$(MVN) dependency:analyze

clean:  ### clean
	@echo $(JAVA_HOME)
	$(MVN) clean
