##
## ckatsak, Tue Oct 20 16:09:38 EEST 2020
##
MVN ?= /opt/apache-maven-3.6.3/bin/mvn
JAVA_HOME ?= /opt/ckatsak/jdk-jvmci/graal-jvmci-8/openjdk1.8.0_265/linux-amd64/product

TARGETS = run run-forked fatjar dep-tree dep-anal clean ejh
.PHONY: help $(TARGETS)

help:  ### Makefile-help-message
	@grep -E '^[a-zA-Z0-9_-]+:.*?### .*$$' $(MAKEFILE_LIST) \
	| sed -n 's/^\(.*\): \(.*\)###\(.*\)/\1\3/p' \
	| column -t  -s ' '
	@echo
	@echo "JAVA_HOME =" $(JAVA_HOME)
	@echo "MVN =" $(MVN)

run: ejh  ### jetty-mvn-plugin
	$(MVN) clean jetty:run

run-forked: ejh  ### jetty-mvn-plugin-fork
	$(MVN) clean jetty:run-forked

fatjar: ejh  ### fat-executable-jar
	$(MVN) clean package assembly:single

dep-tree: ejh  ### dependency-tree
	$(MVN) dependency:tree

dep-anal: ejh  ### dependency-analysis
	$(MVN) dependency:analyze

clean: ejh  ### clean
	$(MVN) clean

ejh:
	@echo $(JAVA_HOME)

