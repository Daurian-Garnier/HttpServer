package httpserver.itf.impl;

import httpserver.itf.HttpRicmlet;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;


public class Application {

    // we make the choice to let the user specify the package when calling an application.
    // we could also add the standard package name, that is "countApp" for our applications, but lose modularity
    // examples : http://localhost:8080/ricmlets/App1/countApp.CountBySessionRicmlet
    // http://localhost:8080/ricmlets/App2/countApp.CountBySessionRicmlet

    private static final HashMap<String, URLClassLoader> loaders = new HashMap<>();
    private static final HashMap<String, HttpRicmlet> ricmlets = new HashMap<>();
    private static final String APP_FILE = System.getProperty("user.dir") + File.separator + "ricm.web.GIVEN"+File.separator+ "jars" +File.separator;

    /*
     * Allow to get the instance of the ricmlet associated to the class className, for the account of the
     * application appName.
     */
    public HttpRicmlet getInstance(String className, String appName, ClassLoader parentClassLoader) throws Exception {

        String key = appName + ":" + className;

        if (ricmlets.containsKey(key)) {
            return ricmlets.get(key);
        }

        URLClassLoader appCL;
        if (loaders.containsKey(appName)) {
            appCL = loaders.get(appName);
        } else {
            //    To create a classloader that will load all the ricmlet classes for a given application, assuming these classes are in
            //    a jar file appJarFile:
            File appJarFile = new File(APP_FILE, appName + ".jar");
            appCL = new URLClassLoader(
                    new URL[]{appJarFile.toURI().toURL()}, parentClassLoader);
            loaders.put(appName, appCL);
        }
        Class<?> c = appCL.loadClass(className);
        HttpRicmlet ricmlet = (HttpRicmlet) c.getDeclaredConstructor().newInstance();
        ricmlets.put(key, ricmlet);

        return ricmlet;
    }
}
