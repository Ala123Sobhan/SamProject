build-HelloWorldFunctionUberJar:
 	mvn clean package
 	mkdir -p $(ARTIFACTS_DIR)/lib
 	cp ./target/my-project*.jar $(ARTIFACTS_DIR)/lib/