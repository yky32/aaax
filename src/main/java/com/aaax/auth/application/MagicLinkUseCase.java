package com.aaax.auth.application;

import java.util.Map;

import com.aaax.account.application.AccountQueries;
import com.aaax.auth.MagicLinkService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class MagicLinkUseCase {

    private final MagicLinkService magicLinks;
    private final AccountQueries queries;
    private final FinishAuthenticatedSession finish;

    public MagicLinkUseCase(
            MagicLinkService magicLinks, AccountQueries queries, FinishAuthenticatedSession finish) {
        this.magicLinks = magicLinks;
        this.queries = queries;
        this.finish = finish;
    }

    public Map<String, Object> request(RequestCommand cmd) {
        return magicLinks.request(cmd.identifier());
    }

    public Map<String, Object> consume(
            ConsumeCommand cmd, HttpServletRequest request, HttpServletResponse response) {
        String username = magicLinks.consume(cmd.token())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid or expired magic link"));
        return finish.execute(queries.requireEntityByUsername(username), "magic_link", request, response, true);
    }

    public record RequestCommand(@NotBlank @Size(max = 128) String identifier) {
    }

    public record ConsumeCommand(@NotBlank @Size(max = 128) String token) {
    }
}
