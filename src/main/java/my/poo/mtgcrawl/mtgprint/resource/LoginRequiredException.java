package my.poo.mtgcrawl.mtgprint.resource;

/** Die Aktion braucht ein angemeldetes Konto (Antwort 401, Code auth.loginRequired). */
public class LoginRequiredException extends RuntimeException {

    public LoginRequiredException() {
        super("Log in required.");
    }
}
