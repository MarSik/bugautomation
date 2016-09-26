package org.marsik.bugautomation.bugzilla;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthorizationCallback {
    String name;
    String password;
}
