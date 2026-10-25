const fs = require('fs');

const collection = {
    info: {
        name: "SARMS Full API Collection",
        description: "Comprehensive testing collection for the SARMS Spring Boot Backend",
        schema: "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
    },
    variable: [
        { key: "base_url", value: "http://localhost:8080", type: "string" },
        { key: "adminUserId", value: "ADM-001", type: "string" },
        { key: "adminPassword", value: "password123", type: "string" },
        { key: "studentRollNo", value: "STUD-2025-004", type: "string" },
        { key: "studentPassword", value: "password123", type: "string" },
        { key: "token", value: "", type: "string" },
        { key: "adminToken", value: "", type: "string" },
        { key: "courseCode", value: "CCC101", type: "string" },
        { key: "facultyId", value: "FAC-CSE-001", type: "string" }
    ],
    item: []
};

// Helper to create an item
function createRequest(name, method, urlPath, authType, bodyData, testsStr) {
    if (!urlPath.startsWith('/api')) {
        urlPath = '/api' + urlPath;
    }
    const item = {
        name: name,
        request: {
            method: method,
            header: [
                { key: "Content-Type", value: "application/json" }
            ],
            url: {
                raw: "{{base_url}}" + urlPath,
                host: ["{{base_url}}"],
                path: urlPath.split('/').filter(p => p !== '')
            }
        },
        event: [
            {
                listen: "test",
                script: {
                    type: "text/javascript",
                    exec: testsStr.split('\n')
                }
            }
        ]
    };

    if (authType === 'bearer') {
        item.request.auth = {
            type: "bearer",
            bearer: [{ key: "token", value: "{{token}}", type: "string" }]
        };
    } else if (authType === 'adminAuth') {
        item.request.auth = {
            type: "bearer",
            bearer: [{ key: "token", value: "{{adminToken}}", type: "string" }]
        };
    }

    if (bodyData) {
        item.request.body = {
            mode: "raw",
            raw: JSON.stringify(bodyData, null, 4),
            options: { raw: { language: "json" } }
        };
    }

    return item;
}

// 1. AUTHENTICATION
const authItems = [];

// Login Admin
authItems.push(createRequest(
    "1. Auth - Login Admin (Success)", "POST", "/auth/login", "none",
    { userId: "{{adminUserId}}", password: "{{adminPassword}}" },
    `pm.test("Status code is 200", function () { pm.response.to.have.status(200); });
pm.test("Response must be JSON", function () { pm.response.to.be.json; });
var jsonData = pm.response.json();
pm.test("Response has token", function () { pm.expect(jsonData).to.have.property('token'); });
pm.test("Response has correct role", function () { pm.expect(jsonData.role).to.eql("admin"); });
pm.environment.set("adminToken", jsonData.token);
pm.environment.set("token", jsonData.token); // set default token to admin for now`
));

// Login Fail
authItems.push(createRequest(
    "2. Auth - Login (Fail - Bad Credentials)", "POST", "/auth/login", "none",
    { userId: "invalid_user", password: "wrong_password" },
    `pm.test("Status code is 401", function () { pm.response.to.have.status(401); });
var jsonData = pm.response.json();
pm.test("Returns error message", function () { pm.expect(jsonData).to.have.property('error'); });`
));

// Me (Success)
authItems.push(createRequest(
    "3. Auth - Get Current User (Me)", "GET", "/auth/me", "bearer", null,
    `pm.test("Status code is 200", function () { pm.response.to.have.status(200); });
var jsonData = pm.response.json();
pm.test("Has userId", function () { pm.expect(jsonData).to.have.property('userId'); });`
));

// Me (Fail)
authItems.push(createRequest(
    "4. Auth - Me (Fail Unauthorized)", "GET", "/auth/me", "none", null,
    `pm.test("Status code is 401 or 403", function () { pm.expect(pm.response.code).to.be.oneOf([401,403]); });`
));

// Forgot Password
authItems.push(createRequest(
    "5. Auth - Forgot Password", "POST", "/auth/forgot-password", "none",
    { userId: "{{studentRollNo}}", email: "student.jxhjqn@mrca.edu", newPassword: "password123" },
    `pm.test("Status is 200", function() { pm.response.to.have.status(200); });`
));

collection.item.push({ name: "1. Authentication", item: authItems });

// 2. STUDENTS
const studentItems = [];

studentItems.push(createRequest(
    "1. Students - Get All", "GET", "/students", "bearer", null,
    `pm.test("Status code is 200", function () { pm.response.to.have.status(200); });
pm.test("Response is array", function () { pm.expect(pm.response.json()).to.be.an('array'); });`
));

studentItems.push(createRequest(
    "2. Students - Register Student (Admin Path)", "POST", "/students/register", "adminAuth",
    {
        fullName: "Test Student",
        fatherName: "Mr Test",
        motherName: "Mrs Test",
        guardianPhone: "1234567890",
        personalPhone: "0987654321",
        houseNo: "123",
        street: "Test Ave",
        city: "Tech City",
        state: "State",
        pinCode: "123456",
        dob: "2005-01-01",
        batchYear: 2026,
        program: "BTech - CSE"
    },
    `pm.test("Status code is 200", function () { pm.response.to.have.status(200); });
var jsonData = pm.response.json();
pm.test("Has rollNo", function () { pm.expect(jsonData).to.have.property('rollNo'); });
if (jsonData.rollNo) {
    pm.environment.set("studentRollNo", jsonData.rollNo);
    pm.environment.set("studentPassword", jsonData.temporaryPassword);
}`
));

// Login as student to test restrictions
studentItems.push(createRequest(
    "3. Auth - Login as new Student", "POST", "/auth/login", "none",
    { userId: "{{studentRollNo}}", password: "{{studentPassword}}" },
    `pm.test("Status 200", function () { pm.response.to.have.status(200); });
var jsonData = pm.response.json();
pm.environment.set("studentToken", jsonData.token);
pm.environment.set("token", jsonData.token);`
));

studentItems.push(createRequest(
    "4. Students - Register Student (Fail - Not Admin)", "POST", "/students/register", "bearer",
    {
        fullName: "Should Fail", batchYear: 2026, program: "BTech - CSE", dob: "2005-01-01"
    },
    `pm.test("Status code is 403 Forbidden", function () { pm.response.to.have.status(403); });`
));

studentItems.push(createRequest(
    "5. Students - Get By RollNo", "GET", "/students/{{studentRollNo}}", "bearer", null,
    `pm.test("Status code is 200", function () { pm.response.to.have.status(200); });`
));

studentItems.push(createRequest(
    "6. Students - Update Profile", "PUT", "/students/{{studentRollNo}}", "bearer",
    {
        personalPhone: "555-555-5555"
    },
    `pm.test("Status 200", function () { pm.response.to.have.status(200); });`
));

studentItems.push(createRequest(
    "7. Students - Enroll", "POST", "/students/{{studentRollNo}}/enroll", "bearer",
    {
        semester: 1,
        courseCodes: ["{{courseCode}}"]
    },
    `pm.test("Status 200 or 400 (if invalid course)", function () { pm.expect(pm.response.code).to.be.oneOf([200, 400]); });`
));

studentItems.push(createRequest(
    "8. Students - Update Grades (Admin Only)", "PUT", "/students/{{studentRollNo}}/grades", "adminAuth",
    {
        semester: 1,
        courses: [
            { courseCode: "{{courseCode}}", newGrade: "A" }
        ]
    },
    `pm.test("Status code is 200", function () { pm.response.to.have.status(200); });`
));

collection.item.push({ name: "2. Students", item: studentItems });

// 3. COURSES
const courseItems = [];

courseItems.push(createRequest(
    "1. Courses - Get All", "GET", "/courses", "bearer", null,
    `pm.test("Status code is 200", function () { pm.response.to.have.status(200); });
var data = pm.response.json();
if(data.length > 0) { pm.environment.set("courseCode", data[0].code); }`
));

courseItems.push(createRequest(
    "2. Courses - Get Filtered (Semester)", "GET", "/courses?semester=1", "bearer", null,
    `pm.test("Status code is 200", function () { pm.response.to.have.status(200); });`
));

courseItems.push(createRequest(
    "3. Courses - Get By Code", "GET", "/courses/{{courseCode}}", "bearer", null,
    `pm.test("Status code 200", function () { pm.response.to.have.status(200); });`
));

courseItems.push(createRequest(
    "4. Courses - Toggle Status", "PUT", "/courses/{{courseCode}}/status", "bearer", null,
    `pm.test("Status code 200", function () { pm.response.to.have.status(200); });`
));

// Switch token back to Admin
courseItems.push(createRequest(
    "5. Courses - Publish Results", "PUT", "/courses/publish", "adminAuth",
    { semesters: [1] },
    `pm.test("Status code 200", function () { pm.response.to.have.status(200); });`
));

collection.item.push({ name: "3. Courses", item: courseItems });

// 4. MARKS
const marksItems = [];

marksItems.push(createRequest(
    "1. Marks - Get By Course", "GET", "/marks/{{courseCode}}", "bearer", null,
    `pm.test("Status code 200", function () { pm.response.to.have.status(200); });`
));

marksItems.push(createRequest(
    "2. Marks - Update Components", "PUT", "/marks/{{courseCode}}/components", "bearer",
    [
        { id: "comp1", name: "Assignments", weight: 30 },
        { id: "comp2", name: "Final", weight: 70 }
    ],
    `pm.test("Status code 200 or 400 (if activeSemester=false)", function () { pm.expect(pm.response.code).to.be.oneOf([200, 400]); });`
));

marksItems.push(createRequest(
    "3. Marks - Update Student Marks", "PUT", "/marks/{{courseCode}}", "bearer",
    [
        {
            rollNo: "{{studentRollNo}}",
            name: "Test Student",
            marks: { "comp1": 25, "comp2": 65 }
        }
    ],
    `pm.test("Status code 200 or 400", function () { pm.expect(pm.response.code).to.be.oneOf([200, 400]); });`
));

collection.item.push({ name: "4. Marks", item: marksItems });

// 5. ADMIN
const adminItems = [];

adminItems.push(createRequest(
    "1. Admin - Upload Results Bulk", "POST", "/admin/upload-results", "adminAuth",
    { userId: "{{adminUserId}}", password: "{{adminPassword}}" },
    `pm.test("Status code 200", function () { pm.response.to.have.status(200); });`
));

adminItems.push(createRequest(
    "2. Admin - Upload Results (Fail - Wrong Pwd)", "POST", "/admin/upload-results", "adminAuth",
    { userId: "{{adminUserId}}", password: "wrong" },
    `pm.test("Status code 401", function () { pm.response.to.have.status(401); });`
));

collection.item.push({ name: "5. Admin", item: adminItems });

// 6. ENROLLMENTS
const enrollItems = [];

enrollItems.push(createRequest(
    "1. Enrollments - Register", "POST", "/enrollments/register", "bearer",
    { semester: 1, courseCodes: ["{{courseCode}}"] },
    `// Switch token back to student
pm.environment.set("token", pm.environment.get("studentToken"));
pm.test("Status code 200", function () { pm.expect(pm.response.code).to.be.oneOf([200, 400]); });`
));

enrollItems.push(createRequest(
    "2. Enrollments - Get By Student", "GET", "/enrollments/student/{{studentRollNo}}", "bearer", null,
    `pm.test("Status 200", function () { pm.response.to.have.status(200); });`
));

collection.item.push({ name: "6. Enrollments", item: enrollItems });

// 7. PUBLIC / META DATA
const publicItems = [];

publicItems.push(createRequest(
    "1. Departments - Get All", "GET", "/departments", "none", null,
    `pm.test("Status 200", function () { pm.response.to.have.status(200); });`
));

publicItems.push(createRequest(
    "2. Config - Get Current Semester", "GET", "/config/currentSemester", "none", null,
    `pm.test("Status 200", function () { pm.response.to.have.status(200); });`
));

publicItems.push(createRequest(
    "3. Faculty - Get All", "GET", "/faculty", "bearer", null,
    `pm.test("Status 200", function () { pm.response.to.have.status(200); });
var data = pm.response.json();
if(data.length > 0) { pm.environment.set("facultyId", data[0].facultyId); }`
));

collection.item.push({ name: "7. Meta / Departments / Faculty", item: publicItems });

// 8. ANALYTICS & USAGE (solo addition after fork). Runs after 6. Enrollments, so {{token}} is the student token.
const analyticsItems = [];

analyticsItems.push(createRequest(
    "1. Analytics - Course Outcomes (Admin)", "GET", "/analytics/courses", "adminAuth", null,
    `pm.test("Status 200", function () { pm.response.to.have.status(200); });
var data = pm.response.json();
pm.test("Has demoData flag and rows", function () {
    pm.expect(data).to.have.property("demoData");
    pm.expect(data.rows).to.be.an("array");
});`
));

analyticsItems.push(createRequest(
    "2. Analytics - Needs Attention (Admin)", "GET", "/analytics/attention", "adminAuth", null,
    `pm.test("Status 200", function () { pm.response.to.have.status(200); });
pm.test("Every flag has a reason", function () {
    pm.response.json().rows.forEach(function (r) { pm.expect(r.reasons.length).to.be.above(0); });
});`
));

analyticsItems.push(createRequest(
    "3. Analytics - Course Outcomes as Student (Forbidden)", "GET", "/analytics/courses", "bearer", null,
    `pm.test("Status 403", function () { pm.response.to.have.status(403); });`
));

analyticsItems.push(createRequest(
    "4. Analytics - Own Progress (Student)", "GET", "/analytics/progress/{{studentRollNo}}", "bearer", null,
    `pm.test("Status 200 or 404", function () { pm.expect(pm.response.code).to.be.oneOf([200, 404]); });`
));

analyticsItems.push(createRequest(
    "5. Analytics - Other Student's Progress (Forbidden)", "GET", "/analytics/progress/DEMO-CSE-01", "bearer", null,
    `pm.test("Status 403", function () { pm.response.to.have.status(403); });`
));

analyticsItems.push(createRequest(
    "6. Usage - Record Page View (Student)", "POST", "/usage/events", "bearer",
    { route: "/student/progress" },
    `pm.test("Status 204", function () { pm.response.to.have.status(204); });`
));

analyticsItems.push(createRequest(
    "7. Usage - Record Another Portal's Page (Rejected)", "POST", "/usage/events", "bearer",
    { route: "/admin/analytics" },
    `pm.test("Status 400", function () { pm.response.to.have.status(400); });`
));

analyticsItems.push(createRequest(
    "8. Usage - Summary (Admin)", "GET", "/usage/summary?days=30", "adminAuth", null,
    `pm.test("Status 200", function () { pm.response.to.have.status(200); });
pm.test("30 zero-filled days", function () { pm.expect(pm.response.json().daily.length).to.eql(30); });`
));

analyticsItems.push(createRequest(
    "9. Usage - Summary as Student (Forbidden)", "GET", "/usage/summary", "bearer", null,
    `pm.test("Status 403", function () { pm.response.to.have.status(403); });`
));

collection.item.push({ name: "8. Analytics & Usage (solo addition)", item: analyticsItems });

// 9. SECURITY FIX (solo addition after fork): password reset needs the email on file; admins can't self-reset
const securityItems = [];

securityItems.push(createRequest(
    "1. Reset - Admin by ID Only (Rejected)", "POST", "/auth/forgot-password", "none",
    { userId: "ADM-001", email: "admin@mrca.edu", newPassword: "hacked123" },
    `pm.test("Status 400", function () { pm.response.to.have.status(400); });`
));

securityItems.push(createRequest(
    "2. Reset - Admin Password Unchanged", "POST", "/auth/login", "none",
    { userId: "{{adminUserId}}", password: "{{adminPassword}}" },
    `pm.test("Admin can still log in with the original password", function () { pm.response.to.have.status(200); });`
));

securityItems.push(createRequest(
    "3. Reset - Student with Wrong Email (Rejected)", "POST", "/auth/forgot-password", "none",
    { userId: "DEMO-CSE-02", email: "attacker@example.com", newPassword: "hacked123" },
    `pm.test("Status 400", function () { pm.response.to.have.status(400); });`
));

securityItems.push(createRequest(
    "4. Reset - Student with Email on File", "POST", "/auth/forgot-password", "none",
    { userId: "DEMO-CSE-02", email: "demo-cse-02@demo.invalid", newPassword: "password123" },
    `pm.test("Status 200", function () { pm.response.to.have.status(200); });`
));

collection.item.push({ name: "9. Security Fix: Password Reset (solo addition)", item: securityItems });

// Write the file
fs.writeFileSync('SARMS_Postman_Collection.json', JSON.stringify(collection, null, 2));
console.log("Postman collection generated successfully.");
