import androidx.annotation.UiThread;

public class ThreadFlow {
    @UiThread
    public void myUiMethod() {
    }

    public void myRandomMethod() {
        // Unconditional call: this method requires UI thread too
        myUiMethod();
    }

    public void myOtherMethod1(int x) {
        // Conditional call - won't infer UI thread
        if (x > 5) {
            myUiMethod();
        }
    }

    public void myOtherMethod2(int x) {
        // Conditional call - won't infer UI thread
        if (x > 10) {
            return;
        }

        myUiMethod();
    }
}
