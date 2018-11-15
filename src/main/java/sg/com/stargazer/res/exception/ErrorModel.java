//Turn of CHECKSTYLE issues
//CHECKSTYLE IGNORE CustomImportOrder FOR NEXT 100 LINES
//CHECKSTYLE IGNORE LineLength FOR NEXT 100 LINES
//CHECKSTYLE IGNORE Indentation FOR NEXT 100 LINES
package sg.com.stargazer.res.exception;

import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Wither;

@Data
@EqualsAndHashCode(callSuper = false)
@Generated("dashur-swagger-generator")
@Builder
public class ErrorModel {
    /**
     * 
     */
    @JsonProperty("type")
    private String type;
    /**
     * 
     */
    @JsonProperty("code")
    private Integer code;
    /**
     * 
     */
    @JsonProperty("message")
    private String message;
    /**
     * 
     */
    @JsonProperty("fields")
    private List<ErrorFieldModel> fields;
}