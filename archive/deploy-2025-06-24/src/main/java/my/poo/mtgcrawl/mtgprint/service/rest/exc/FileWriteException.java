/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.service.rest.exc;

import my.poo.mtgcrawl.mtgprint.service.rest.exc.WrapperException;

public class FileWriteException
extends Exception
implements WrapperException {
    private final Exception actual;

    @Override
    public Exception getActual() {
        return this.actual;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof FileWriteException)) {
            return false;
        }
        FileWriteException other = (FileWriteException)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Exception this$actual = this.getActual();
        Exception other$actual = other.getActual();
        return !(this$actual == null ? other$actual != null : !this$actual.equals(other$actual));
    }

    protected boolean canEqual(Object other) {
        return other instanceof FileWriteException;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Exception $actual = this.getActual();
        result = result * 59 + ($actual == null ? 43 : $actual.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "FileWriteException(actual=" + String.valueOf(this.getActual()) + ")";
    }

    public FileWriteException(Exception actual) {
        this.actual = actual;
    }
}

