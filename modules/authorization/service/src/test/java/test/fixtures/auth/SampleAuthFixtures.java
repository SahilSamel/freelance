package test.fixtures.auth;

import com.freelance.rest.app.endpoint.annotation.Authorization;

public class SampleAuthFixtures {

    @Authorization(key = "", name = "Name", group = "Group", description = "Desc")
    public void emptyKey() {}

    @Authorization(key = "KEY_NO_NAME", name = "", group = "Group", description = "Desc")
    public void emptyName() {}

    @Authorization(key = "KEY_NO_GROUP", name = "Name", group = "", description = "Desc")
    public void emptyGroup() {}

    @Authorization(key = "SAME_KEY", name = "Name 1", group = "Group 1", description = "Desc 1")
    public void dup1() {}

    @Authorization(key = "SAME_KEY", name = "Name 2", group = "Group 2", description = "Desc 2")
    public void dup2() {}
}
