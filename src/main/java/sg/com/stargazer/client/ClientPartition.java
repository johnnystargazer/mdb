package sg.com.stargazer.client;

import java.util.Date;

import lombok.Value;

@Value
public class ClientPartition {
    private Integer partition;
    private Date startDate;
//find . type -f | xargs grep -l 19201020320 | xargs cp -t /tmp    
//find . -type f -name "*2018-11-01T01*" -exec cp '{}' /tmp \;
    public void start(ClientConfig clientConfig, Date date) {
        
        
        
        
    }
}
