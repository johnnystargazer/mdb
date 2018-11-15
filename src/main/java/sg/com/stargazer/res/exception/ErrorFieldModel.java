//Turn of CHECKSTYLE issues
//CHECKSTYLE IGNORE CustomImportOrder FOR NEXT 100 LINES
//CHECKSTYLE IGNORE LineLength FOR NEXT 100 LINES
//CHECKSTYLE IGNORE Indentation FOR NEXT 100 LINES
package sg.com.stargazer.res.exception;

import javax.annotation.Generated;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Generated("dashur-swagger-generator")
public class ErrorFieldModel {
    /**
     * 
     */
    @JsonProperty("field")
    private String field;
    /**
     * 
     */
    @JsonProperty("message")
    private String message;
}