package pl.ninecube.slicer;

import java.util.Map;

public class ServiceResolver extends PackageScanner implements Resolver {

    public ServiceResolver(Class<?> classType) {
        registerClasses(this.getClass().getPackage(), classType);
    }

    @Override
    public Class<?> resolveClassByName(String name) {
        Map<String, Class<?>> classMap = getClassMap();
        return classMap.get(name.toLowerCase());
    }
}
