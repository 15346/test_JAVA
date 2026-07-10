// ============================================================
//  前端逻辑：用 fetch 调用后端的 REST 接口（标准的前后端分离做法）
// ============================================================

// 后端接口基础地址（后端跑在 8081 端口）
const API = "http://localhost:8081/api/todos";

const listEl = document.getElementById("list");
const titleEl = document.getElementById("title");
const addBtn = document.getElementById("addBtn");

document.getElementById("apiBase").textContent = API;

// ---------- 获取并渲染全部待办 (GET) ----------
async function load() {
    const res = await fetch(API);
    const todos = await res.json();
    listEl.innerHTML = "";
    todos.forEach(render);
}

// 渲染单条待办到页面上
function render(todo) {
    const li = document.createElement("li");
    li.className = todo.done ? "done" : "";

    // 复选框：勾选时切换完成状态 (PUT)
    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.checked = todo.done;
    checkbox.onchange = () => toggleDone(todo);

    // 标题文字
    const span = document.createElement("span");
    span.className = "title";
    span.textContent = todo.title;

    // 删除按钮 (DELETE)
    const del = document.createElement("button");
    del.textContent = "删除";
    del.className = "del";
    del.onclick = () => remove(todo.id);

    li.append(checkbox, span, del);
    listEl.appendChild(li);
}

// ---------- 新增 (POST) ----------
async function add() {
    const title = titleEl.value.trim();
    if (!title) return;
    await fetch(API, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title })
    });
    titleEl.value = "";
    load();
}

// ---------- 切换完成状态 (PUT) ----------
async function toggleDone(todo) {
    await fetch(`${API}/${todo.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ title: todo.title, done: !todo.done })
    });
    load();
}

// ---------- 删除 (DELETE) ----------
async function remove(id) {
    await fetch(`${API}/${id}`, { method: "DELETE" });
    load();
}

// ---------- 事件绑定 ----------
addBtn.onclick = add;
titleEl.addEventListener("keydown", (e) => {
    if (e.key === "Enter") add();
});

// 页面打开时先加载一次
load();
