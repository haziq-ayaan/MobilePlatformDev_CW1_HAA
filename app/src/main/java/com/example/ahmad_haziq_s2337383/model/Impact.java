// Name                 Haziq Ayaan Ahmad
// Student ID           S2337383
// Programme of Study   BSc Computing (Hons)

package com.example.ahmad_haziq_s2337383.model;

/**
 * The <impact> block of a situation: lanes affected and/or delays.
 * Every field is optional in the feed, so Integer (nullable) is used for lane counts.
 */
public class Impact {
    private Integer operationalLanes;  // <numberOfOperationalLanes>
    private Integer lanesRestricted;   // <numberOfLanesRestricted>
    private DelayLevel delayLevel = DelayLevel.NONE; // <delays><delaysType>

    public Integer getOperationalLanes() {
        return operationalLanes;
    }

    public void setOperationalLanes(Integer operationalLanes) {
        this.operationalLanes = operationalLanes;
    }

    public Integer getLanesRestricted() {
        return lanesRestricted;
    }

    public void setLanesRestricted(Integer lanesRestricted) {
        this.lanesRestricted = lanesRestricted;
    }

    public DelayLevel getDelayLevel() {
        return delayLevel;
    }

    public void setDelayLevel(DelayLevel delayLevel) {
        this.delayLevel = delayLevel;
    }

    public boolean isEmpty() {
        return operationalLanes == null && lanesRestricted == null && delayLevel == DelayLevel.NONE;
    }

    /** A one-line readable summary, e.g. "No lanes open · Long delays". */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        if (operationalLanes != null) {
            sb.append(operationalLanes == 0 ? "No lanes open"
                    : operationalLanes + (operationalLanes == 1 ? " lane open" : " lanes open"));
        }
        if (lanesRestricted != null) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(lanesRestricted).append(lanesRestricted == 1 ? " lane restricted" : " lanes restricted");
        }
        if (delayLevel != DelayLevel.NONE) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(delayLevel.getLabel());
        }
        return sb.length() == 0 ? "No impact reported" : sb.toString();
    }
}
