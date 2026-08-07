package auth.pymes.common.models.enums;

import lombok.Getter;

@Getter
public enum RoleName {

    OWNER(4),
    ADMIN(3),
    CONTABLE(2),
    VIEWER(1);

    private final int weight;

    RoleName(int weight) {
        this.weight = weight;
    }

    public boolean hasMorePowerThan(RoleName other) {
        return this.weight > other.weight;
    }

    public boolean hasAtLeastPowerOf(RoleName other) {
        return this.weight >= other.weight;
    }
}
