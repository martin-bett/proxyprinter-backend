/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy.dto;

import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;

public class RatingRequest {
    private ApiConfig configId;
    private int score;
    private String comment;
    private String action;

    public ApiConfig getConfigId() {
        return this.configId;
    }

    public int getScore() {
        return this.score;
    }

    public String getComment() {
        return this.comment;
    }

    public String getAction() {
        return this.action;
    }

    public void setConfigId(ApiConfig configId) {
        this.configId = configId;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof RatingRequest)) {
            return false;
        }
        RatingRequest other = (RatingRequest)o;
        if (!other.canEqual(this)) {
            return false;
        }
        if (this.getScore() != other.getScore()) {
            return false;
        }
        ApiConfig this$configId = this.getConfigId();
        ApiConfig other$configId = other.getConfigId();
        if (this$configId == null ? other$configId != null : !this$configId.equals(other$configId)) {
            return false;
        }
        String this$comment = this.getComment();
        String other$comment = other.getComment();
        if (this$comment == null ? other$comment != null : !this$comment.equals(other$comment)) {
            return false;
        }
        String this$action = this.getAction();
        String other$action = other.getAction();
        return !(this$action == null ? other$action != null : !this$action.equals(other$action));
    }

    protected boolean canEqual(Object other) {
        return other instanceof RatingRequest;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        result = result * 59 + this.getScore();
        ApiConfig $configId = this.getConfigId();
        result = result * 59 + ($configId == null ? 43 : $configId.hashCode());
        String $comment = this.getComment();
        result = result * 59 + ($comment == null ? 43 : $comment.hashCode());
        String $action = this.getAction();
        result = result * 59 + ($action == null ? 43 : $action.hashCode());
        return result;
    }

    public String toString() {
        return "RatingRequest(configId=" + String.valueOf(this.getConfigId()) + ", score=" + this.getScore() + ", comment=" + this.getComment() + ", action=" + this.getAction() + ")";
    }
}

