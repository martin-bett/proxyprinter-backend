/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 */
package my.poo.mtgcrawl.mtgprint.service;

import jakarta.servlet.http.HttpServletRequest;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;

public class RequestUtils {
    public static VisitorIdentifier extractVisitorIdentifier(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String fingerprint = request.getHeader("X-Fingerprint");
        return new VisitorIdentifier(ip, fingerprint);
    }
}

