package vanillacord.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

public class AuthlibShims {

    /** Checks if a class has a public method with the given name and parameters */
    public static boolean hasMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            clazz.getMethod(methodName, parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /** Checks if a class has a public constructor with the given parameter types */
    private static boolean hasConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        try {
            clazz.getConstructor(parameterTypes);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Creates a GameProfile with the given ID, name, and properties.
     * Uses the newer constructor if available; otherwise falls back to the old method.
     */
    public static GameProfile shimProfile(UUID id, String name, Multimap<String, Property> properties) {
        try {
            if (hasConstructor(GameProfile.class, UUID.class, String.class, PropertyMap.class)) {
                // We have to use reflection here as we don't have the newer version of authlib at build time, and therefore static type analysis would throw
                PropertyMap map = PropertyMap.class.getConstructor(Multimap.class).newInstance(properties);
                Constructor<GameProfile> constructor = GameProfile.class.getConstructor(UUID.class, String.class, PropertyMap.class);
                return constructor.newInstance(id, name, map);
            }
        } catch (Exception e) {}

        // Old authlib versions don't let you specify the properties, and instead have you mutate the properties map
        GameProfile profile = new GameProfile(id, name);
        profile.getProperties().putAll(properties);
        return profile;
    }

    public static String getName(Property property) {
        if (hasMethod(property.getClass(), "getName")) {
            return property.getName();
        }
        try {
            Method m = property.getClass().getMethod("name");
            return (String) m.invoke(property);
        } catch (Exception e) {
            return null; // Should never happen
        }
    }

    public static String getValue(Property property) {
        if (hasMethod(property.getClass(), "getValue")) {
            return property.getValue();
        }
        try {
            Method m = property.getClass().getMethod("value");
            return (String) m.invoke(property);
        } catch (Exception e) {
            return null; // Should never happen
        }
    }
}