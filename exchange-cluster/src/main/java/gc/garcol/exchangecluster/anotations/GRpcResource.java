package gc.garcol.exchangecluster.anotations;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * @author thaivc
 * @since 2024
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface GRpcResource
{
}
