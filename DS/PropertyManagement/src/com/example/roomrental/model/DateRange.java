package com.example.roomrental.model;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateRange implements Serializable {
    private static final long serialVersionUID = 1L;
    private Date startDate;
    private Date endDate;

    public DateRange(Date startDate, Date endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    // Returns true if this date range is entirely contained within the given date range
    public boolean isContainedWithin(DateRange other) {
        return !this.startDate.before(other.getStartDate()) && !this.endDate.after(other.getEndDate());
    }
    
    public boolean overlaps(DateRange other) {
        return !(endDate.before(other.startDate) || startDate.after(other.endDate));
    }
    
    public DateRange(String text) throws ParseException {
        String[] parts = text.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid date range format: " + text);
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date startDate = dateFormat.parse(parts[0]);
        Date endDate = dateFormat.parse(parts[1]);
        this.startDate=startDate;
        this.endDate=endDate;
    }
    
    public String toString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        String startDateStr = dateFormat.format(startDate);
        String endDateStr = dateFormat.format(endDate);
        return startDateStr + " - " + endDateStr + "\n";
    }
    
}
