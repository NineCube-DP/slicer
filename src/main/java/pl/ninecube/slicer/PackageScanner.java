package pl.ninecube.slicer;

import lombok.Getter;
import org.reflections.Reflections;
import pl.ninecube.slicer.annotation.NamedStruct;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@Getter
public class PackageScanner {

    protected Map<String, Class<?>> classMap = new HashMap<>();

    protected void registerClasses(Package aPackage, Class<?> classType) {
        Reflections reflections = new Reflections(aPackage);
        HashSet<Class<?>> classes = (HashSet<Class<?>>) reflections.getSubTypesOf(classType);

        for (Class<?> aClass : classes) {
            if(aClass.isAnnotationPresent(NamedStruct.class)){
                classMap.put(aClass.getAnnotation(NamedStruct.class).value(), aClass);
            }else{
                classMap.put(aClass.getSimpleName().toLowerCase(), aClass);
            }
        }


        System.out.println();

    }
}
