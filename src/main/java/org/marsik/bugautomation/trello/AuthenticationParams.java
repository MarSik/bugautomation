package org.marsik.bugautomation.trello;

import javax.ws.rs.QueryParam;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticationParams {
    @QueryParam("key")
    String applicationKey;

    @QueryParam("token")
    String token;
}
