/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy.dto;

public class RatingDto {
    private Long configId;
    private Double scoreAvailability;
    private Double scoreQuality;

    public Long getConfigId() {
        return this.configId;
    }

    public Double getScoreAvailability() {
        return this.scoreAvailability;
    }

    public Double getScoreQuality() {
        return this.scoreQuality;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public void setScoreAvailability(Double scoreAvailability) {
        this.scoreAvailability = scoreAvailability;
    }

    public void setScoreQuality(Double scoreQuality) {
        this.scoreQuality = scoreQuality;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof RatingDto)) {
            return false;
        }
        RatingDto other = (RatingDto)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Long this$configId = this.getConfigId();
        Long other$configId = other.getConfigId();
        if (this$configId == null ? other$configId != null : !((Object)this$configId).equals(other$configId)) {
            return false;
        }
        Double this$scoreAvailability = this.getScoreAvailability();
        Double other$scoreAvailability = other.getScoreAvailability();
        if (this$scoreAvailability == null ? other$scoreAvailability != null : !((Object)this$scoreAvailability).equals(other$scoreAvailability)) {
            return false;
        }
        Double this$scoreQuality = this.getScoreQuality();
        Double other$scoreQuality = other.getScoreQuality();
        return !(this$scoreQuality == null ? other$scoreQuality != null : !((Object)this$scoreQuality).equals(other$scoreQuality));
    }

    protected boolean canEqual(Object other) {
        return other instanceof RatingDto;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Long $configId = this.getConfigId();
        result = result * 59 + ($configId == null ? 43 : ((Object)$configId).hashCode());
        Double $scoreAvailability = this.getScoreAvailability();
        result = result * 59 + ($scoreAvailability == null ? 43 : ((Object)$scoreAvailability).hashCode());
        Double $scoreQuality = this.getScoreQuality();
        result = result * 59 + ($scoreQuality == null ? 43 : ((Object)$scoreQuality).hashCode());
        return result;
    }

    public String toString() {
        return "RatingDto(configId=" + this.getConfigId() + ", scoreAvailability=" + this.getScoreAvailability() + ", scoreQuality=" + this.getScoreQuality() + ")";
    }
}

