/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfig;

@Entity
@Table(name="apiConfigParameter")
public class ApiConfigParameter {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name="config_id", nullable=false)
    private ApiConfig config;
    @Column
    private String name;
    @Column
    private String value;
    @Column
    private String category;

    public Long getId() {
        return this.id;
    }

    public ApiConfig getConfig() {
        return this.config;
    }

    public String getName() {
        return this.name;
    }

    public String getValue() {
        return this.value;
    }

    public String getCategory() {
        return this.category;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setConfig(ApiConfig config) {
        this.config = config;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

