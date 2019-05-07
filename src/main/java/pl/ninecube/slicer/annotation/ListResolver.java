package pl.ninecube.slicer.annotation;

import pl.ninecube.slicer.Resolver;
import pl.ninecube.slicer.ServiceResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ListResolver {
    Class<? extends Resolver> value() default ServiceResolver.class;
}
