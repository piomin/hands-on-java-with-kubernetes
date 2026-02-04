package pl.piomin.services.kafka.consumer.message;

public class Info {

    private long id;
    private String message;

    public Info() {
    }

    public Info(long id, String message) {
        this.id = id;
        this.message = message;
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}
