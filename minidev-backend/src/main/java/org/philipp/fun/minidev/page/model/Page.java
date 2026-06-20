package org.philipp.fun.minidev.page.model;

import org.philipp.fun.minidev.common.model.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * JPA entity representing a navigable page.
 */
@Entity
@Table(name = "pages")
public class Page extends BaseEntity {

    /** The URL path of the page. */
    @Column(nullable = false, unique = true)
    private String path;

    /** The display title of the page. */
    @Column(nullable = false)
    private String title;

    /** The icon identifier. */
    @Column(length = 64)
    private String icon;

    /** The frontend component name. */
    @Column(name = "component_name", nullable = false)
    private String componentName;

    /** The role required to access this page. */
    @Column(name = "role_required")
    private String roleRequired;

    /** The navigation order. */
    @Column(name = "nav_order")
    private int navOrder;

    /** Whether the page is enabled. */
    @Column(nullable = false)
    private boolean enabled = true;

    /** Default constructor. */
    public Page() {}

    /**
     * Constructs a Page.
     *
     * @param path          the URL path
     * @param title         the display title
     * @param icon          the icon identifier
     * @param componentName the frontend component name
     * @param roleRequired  the required role
     * @param navOrder      the navigation order
     */
    public Page(String path, String title, String icon, String componentName, String roleRequired, int navOrder) {
        this.path = path;
        this.title = title;
        this.icon = icon;
        this.componentName = componentName;
        this.roleRequired = roleRequired;
        this.navOrder = navOrder;
        this.enabled = true;
    }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getComponentName() { return componentName; }
    public void setComponentName(String componentName) { this.componentName = componentName; }
    public String getRoleRequired() { return roleRequired; }
    public void setRoleRequired(String roleRequired) { this.roleRequired = roleRequired; }
    public int getNavOrder() { return navOrder; }
    public void setNavOrder(int navOrder) { this.navOrder = navOrder; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}