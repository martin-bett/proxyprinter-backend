package my.poo.mtgcrawl.mtgprint.service.auth;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Zugriff auf app_user, login_token und user_session (siehe sql/2026-10-07_user_accounts.sql). */
@Repository
public class AuthRepository {

    private static final RowMapper<AppUser> USER = (rs, i) ->
            new AppUser(rs.getLong("id"), rs.getString("email"), rs.getTimestamp("created_at").toLocalDateTime());
    private static final RowMapper<LoginToken> TOKEN = (rs, i) -> new LoginToken(rs.getString("email"),
            rs.getString("token_hash"), rs.getString("code_hash"), rs.getInt("attempts"),
            rs.getTimestamp("expires_at").toLocalDateTime());

    private final JdbcTemplate jdbc;

    public AuthRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Pro Adresse gibt es höchstens einen offenen Login, ein neuer ersetzt den alten. */
    public void replaceLoginToken(LoginToken token) {
        jdbc.update("DELETE FROM proxymaster.login_token WHERE email = ?", token.email());
        jdbc.update("INSERT INTO proxymaster.login_token (token_hash, email, code_hash, attempts, expires_at) VALUES (?, ?, ?, 0, ?)",
                token.tokenHash(), token.email(), token.codeHash(), Timestamp.valueOf(token.expiresAt()));
    }

    public Optional<LoginToken> findLoginTokenByHash(String tokenHash) {
        return jdbc.query("SELECT * FROM proxymaster.login_token WHERE token_hash = ?", TOKEN, tokenHash).stream().findFirst();
    }

    public Optional<LoginToken> findLoginTokenByEmail(String email) {
        return jdbc.query("SELECT * FROM proxymaster.login_token WHERE email = ?", TOKEN, email).stream().findFirst();
    }

    public void incrementAttempts(String email) {
        jdbc.update("UPDATE proxymaster.login_token SET attempts = attempts + 1 WHERE email = ?", email);
    }

    public void deleteLoginToken(String email) {
        jdbc.update("DELETE FROM proxymaster.login_token WHERE email = ?", email);
    }

    public AppUser findOrCreateUser(String email) {
        jdbc.update("INSERT INTO proxymaster.app_user (email) VALUES (?) ON CONFLICT (email) DO NOTHING", email);
        jdbc.update("UPDATE proxymaster.app_user SET last_login = now() WHERE email = ?", email);
        return jdbc.queryForObject("SELECT id, email, created_at FROM proxymaster.app_user WHERE email = ?", USER, email);
    }

    public void insertSession(String tokenHash, long userId, String userAgent) {
        jdbc.update("INSERT INTO proxymaster.user_session (token_hash, user_id, user_agent) VALUES (?, ?, ?)",
                tokenHash, userId, userAgent == null ? null : userAgent.substring(0, Math.min(255, userAgent.length())));
    }

    public Optional<AppUser> findUserBySession(String tokenHash) {
        return jdbc.query("""
                SELECT u.id, u.email, u.created_at FROM proxymaster.user_session s
                JOIN proxymaster.app_user u ON u.id = s.user_id
                WHERE s.token_hash = ? AND s.last_seen > ?""", USER, tokenHash,
                Timestamp.valueOf(LocalDateTime.now().minusDays(AuthService.SESSION_DAYS))).stream().findFirst();
    }

    /** Verlängert die Sitzung, aber höchstens einmal pro Stunde, damit nicht jede Anfrage schreibt. */
    public void touchSession(String tokenHash) {
        jdbc.update("UPDATE proxymaster.user_session SET last_seen = now() WHERE token_hash = ? AND last_seen < now() - interval '1 hour'",
                tokenHash);
    }

    public void deleteSession(String tokenHash) {
        jdbc.update("DELETE FROM proxymaster.user_session WHERE token_hash = ?", tokenHash);
    }

    public void deleteSessionsOf(long userId) {
        jdbc.update("DELETE FROM proxymaster.user_session WHERE user_id = ?", userId);
    }

    /** Löscht das Konto samt Sitzungen (ON DELETE CASCADE). */
    public void deleteUser(long userId) {
        jdbc.update("DELETE FROM proxymaster.app_user WHERE id = ?", userId);
    }

    public int deleteExpired(LocalDateTime now, LocalDateTime sessionCutoff) {
        int tokens = jdbc.update("DELETE FROM proxymaster.login_token WHERE expires_at < ?", Timestamp.valueOf(now));
        int sessions = jdbc.update("DELETE FROM proxymaster.user_session WHERE last_seen < ?", Timestamp.valueOf(sessionCutoff));
        return tokens + sessions;
    }
}
