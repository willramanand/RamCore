package dev.willram.ramcore.permission;

import org.bukkit.command.CommandSender;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class PermissionRequirementTest {

    @Test
    public void nodeChildBuildsPermissionPath() {
        PermissionNode root = Permissions.node("ramcore");

        assertEquals("ramcore.diagnostics", root.child("diagnostics").value());
    }

    @Test
    public void allRequirementNeedsEveryNode() {
        PermissionRequirement requirement = Permissions.all(
                Permissions.node("ramcore.admin"),
                Permissions.node("ramcore.reload")
        );

        assertTrue(requirement.test(sender("ramcore.admin", "ramcore.reload")));
        assertFalse(requirement.test(sender("ramcore.admin")));
    }

    @Test
    public void anyRequirementNeedsOneNode() {
        PermissionRequirement requirement = Permissions.any(
                Permissions.node("ramcore.admin"),
                Permissions.node("ramcore.reload")
        ).denialMessage("<red>No access.");

        assertTrue(requirement.test(sender("ramcore.reload")));
        assertFalse(requirement.test(sender("ramcore.other")));
        assertEquals("<red>No access.", requirement.denialMessage());
    }

    private static CommandSender sender(String... permissions) {
        Set<String> allowed = Set.of(permissions);
        return (CommandSender) Proxy.newProxyInstance(
                PermissionRequirementTest.class.getClassLoader(),
                new Class<?>[]{CommandSender.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("hasPermission") && args != null && args.length == 1 && args[0] instanceof String permission) {
                        return allowed.contains(permission);
                    }

                    if (method.getName().equals("getName")) {
                        return "test";
                    }

                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class) {
                        return false;
                    }
                    if (returnType == int.class) {
                        return 0;
                    }
                    if (returnType == void.class) {
                        return null;
                    }
                    return null;
                }
        );
    }
}
