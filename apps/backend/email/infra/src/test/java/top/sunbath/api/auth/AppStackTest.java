package top.sunbath.api.auth;

import org.junit.jupiter.api.Test;
import software.amazon.awscdk.App;
import software.amazon.awscdk.assertions.Template;
import java.io.File;
import java.util.Collections;

class AppStackTest {

    @Test
    void testAppStack() {
        if (new File(AppStack.functionPath()).exists()) {
            AppStack stack = new AppStack(new App(), "TestEmailAppStack");
            Template template = Template.fromStack(stack);
            template.hasResourceProperties("AWS::Lambda::Function",
                    Collections.singletonMap("Handler", "top.sunbath.api.email.handler.FunctionHandler"));
        }
    }
}