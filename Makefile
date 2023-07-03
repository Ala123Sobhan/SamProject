build-HelloWorldFunctionUberJar:
    mvn clean package
    mkdir -p $(ARTIFACTS_DIR)/lib
    cp ./target/my-project-1.0*.jar $(ARTIFACTS_DIR)/lib/
