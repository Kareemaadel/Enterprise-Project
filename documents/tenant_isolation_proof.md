# How Tenant Isolation Works

This document explains how we keep data separated between tenants in plain English.

## 1. Automatic Filtering
We use Hibernate 6's `@TenantId` feature. This means you don't have to manually add `WHERE tenant_id = ...` to your queries anymore. Hibernate does it for you automatically.

*   **Reading Data**: Hibernate automatically filters every query so you only see your own tenant's data.
*   **Saving Data**: Hibernate automatically adds the correct tenant ID to any new project, task, or job you save.

---

## 2. The UUID Setup
Hibernate has a small quirk: it expects the tenant ID to be a `String`. However, we use `UUIDs` in our database because they are faster and better for indexing.

To fix this, we use a small helper called `UuidStringConverter`. It simply translates the ID between a `String` (for Hibernate) and a `UUID` (for the database) behind the scenes so everything matches up.

---

## 3. Fast Security Checks
When someone tries to access an ID that doesn't belong to them, we want to know if that ID is actually missing or if it just belongs to another tenant.

### Why not use Hibernate for this?
Hibernate is "locked" into the current user's tenant. Checking for data in *other* tenants using Hibernate is slow and requires opening extra connections, which can bog down the app.

### The Solution: A Simple SQL Check
Instead of using Hibernate, we use a simple SQL check (`JdbcTemplate`) inside the `GlobalExceptionHandler`:
1.  **If a resource isn't found**, we run a tiny, raw SQL query to see if the ID exists anywhere in the database.
2.  **If it exists elsewhere**, we know it's a security issue (Tenant B trying to see Tenant A's data) and return a clear error.
3.  **If it doesn't exist at all**, we return a standard "Not Found" error.

This is much faster than Hibernate and only runs when someone requests an ID that doesn't exist in their tenant.
