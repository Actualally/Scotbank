package uk.co.asepstrath.bank.controllers;

import io.jooby.Context;

import io.jooby.annotation.GET;

import io.jooby.annotation.Path;



import static uk.co.asepstrath.bank.Constants.*;

@Path("/logout")
public class LogoutController {

    @GET
    public void logout(Context ctx) {
        var session = ctx.sessionOrNull();
        if (session != null) {
            session.destroy();
        }
        ctx.sendRedirect(ROUTE_LOGIN);
    }
}