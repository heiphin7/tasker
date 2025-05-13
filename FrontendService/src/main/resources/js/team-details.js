let currentTaskId = null;

function openModal(modalId) {
    const userId = [[${userId}]];
    const leaderId = [[${team.leaderId}]];
    if (modalId === 'createTaskModal' && userId !== leaderId) {
        document.querySelector("#errorContainer").innerHTML =
            '<div class="bg-red-50 border border-red-400 text-red-800 px-4 py-3 rounded relative shadow-md">' +
            '<span class="font-bold">Ошибка:</span> Только администратор может создавать задачи.' +
            '</div>';
        return;
    }
    document.getElementById(modalId).classList.remove('hidden');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.add('hidden');
}

function handleLeaveTeam() {
    const userId = [[${userId}]];
    const leaderId = [[${team.leaderId}]];
    const memberCount = [[${#lists.size(members)}]];
    if (userId === leaderId) {
        if (memberCount <= 1) {
            document.getElementById('adminLeaveFormNoSuccessor').submit();
        } else {
            openModal('leaveTeamModal');
        }
    } else {
        document.getElementById('quitForm').submit();
    }
}

// Открытие модалки редактирования при нажатии на иконку "edit"
function openEditTaskModal(button) {
    // Предотвращаем всплытие события клика, чтобы не открылась ещё и модалка деталей
    // (добавили event.stopPropagation() выше)
    const taskId = button.getAttribute('data-task-id');
    const title = button.getAttribute('data-title');
    const description = button.getAttribute('data-description');
    const status = button.getAttribute('data-status');
    const priority = button.getAttribute('data-priority');
    const dueDate = button.getAttribute('data-due-date');
    const creatorId = button.getAttribute('data-creator-id');
    const assigneeId = button.getAttribute('data-assignee-id');

    document.getElementById('editTaskId').value = taskId;
    document.getElementById('editTitle').value = title;
    document.getElementById('editDescription').value = description;
    document.getElementById('editStatus').value = status;
    document.getElementById('editPriority').value = priority;
    document.getElementById('editDueDate').value = dueDate;
    document.getElementById('editCreatorId').value = creatorId;
    document.getElementById('editAssigneeId').value = assigneeId;

    document.getElementById('editTaskModal').classList.remove('hidden');
}

// Открытие модалки деталей задачи
function openTaskDetailsModal(taskElement) {
    const taskId = taskElement.getAttribute('data-task-id');
    const title = taskElement.getAttribute('data-title');
    const description = taskElement.getAttribute('data-description');
    const status = taskElement.getAttribute('data-status');
    const priority = taskElement.getAttribute('data-priority');
    const dueDate = taskElement.getAttribute('data-due-date');
    const creatorId = taskElement.getAttribute('data-creator-id');
    const assigneeId = taskElement.getAttribute('data-assignee-id');
    const subtasksJson = taskElement.getAttribute('data-subtasks');
    const commentsJson = taskElement.getAttribute('data-comments');
    const subtasks = taskElement.getAttribute('data-subtasks')

    // Сохраняем глобально (или в скрытое поле, как вам удобнее):
    currentTaskId = taskId;

    let comments = [];
    try {
        comments = JSON.parse(commentsJson);
    } catch (e) {
        console.error('JSON parse error:', e);
    }

    // Заполняем поля в модалке
    document.getElementById('detailsTitle').textContent = title;
    document.getElementById('detailsDescription').textContent = description;
    document.getElementById('detailsCreator').textContent = creatorId ? creatorId : 'неизвестен';
    document.getElementById('detailsAssignee').textContent = assigneeId ? assigneeId : 'не назначен';
    document.getElementById('detailsDueDate').textContent = dueDate ? dueDate : 'нет дедлайна';


    // Статус
    const statusEl = document.getElementById('detailsStatus');
    statusEl.textContent = status;
    statusEl.className = 'ml-2 px-2 py-1 text-sm rounded'; // сбрасываем стили
    // Зададим простенькие стили в зависимости от статуса
    switch (status) {
        case 'NEW':
            statusEl.classList.add('bg-blue-100','text-blue-800');
            break;
        case 'IN_PROGRESS':
            statusEl.classList.add('bg-yellow-100','text-yellow-800');
            break;
        case 'COMPLETED':
            statusEl.classList.add('bg-green-100','text-green-800');
            break;
        default:
            statusEl.classList.add('bg-gray-100','text-gray-800');
    }

    // Приоритет
    const priorityEl = document.getElementById('detailsPriority');
    priorityEl.textContent = priority;
    priorityEl.className = 'ml-2 px-2 py-1 text-sm rounded';
    switch (priority) {
        case 'LOW':
            priorityEl.classList.add('bg-green-100','text-green-800');
            break;
        case 'MEDIUM':
            priorityEl.classList.add('bg-yellow-100','text-yellow-800');
            break;
        case 'HIGH':
            priorityEl.classList.add('bg-red-100','text-red-800');
            break;
        default:
            priorityEl.classList.add('bg-gray-100','text-gray-800');

            document.getElementById('taskDetailsModal').classList.remove('hidden');
    }

    // Подзадачи
    const subtasksContainer = document.getElementById('detailsSubtasks');
    subtasksContainer.innerHTML = '';
    if (subtasks && subtasks.length > 0) {
        subtasks.forEach(st => {
            const stEl = document.createElement('div');
            stEl.className = 'p-2 bg-gray-50 rounded border';
            stEl.innerHTML = `
          <div class="font-semibold mb-1">${st.title ? st.title : 'Без названия'}</div>
          <div class="text-sm text-gray-600 mb-1">Описание: ${st.description ? st.description : ''}</div>
          <div class="text-sm text-gray-500">Статус: ${st.completed ? 'Выполнено' : 'Не выполнено'}</div>
        `;
            subtasksContainer.appendChild(stEl);
        });
    } else {
        subtasksContainer.innerHTML = `<p class="text-gray-500">Подзадач пока нет</p>`;
    }

    // Комментарии
    const commentsContainer = document.getElementById('detailsComments');
    commentsContainer.innerHTML = '';
    if (comments && comments.length > 0) {
        comments.forEach(cm => {
            const cmEl = document.createElement('div');
            cmEl.className = 'p-2 bg-gray-50 rounded border';
            cmEl.innerHTML = `
          <div class="text-sm text-gray-700 mb-1">${cm.content ? cm.content : ''}</div>
          <div class="text-xs text-gray-500">Автор (userId): ${cm.userId ? cm.userId : '??'} | Дата: ${cm.createdAt ? cm.createdAt : ''}</div>
        `;
            commentsContainer.appendChild(cmEl);
        });
    } else {
        commentsContainer.innerHTML = `<p class="text-gray-500">Комментариев пока нет</p>`;
    }

    // Открываем модалку
    document.getElementById('taskDetailsModal').classList.remove('hidden');
}

function closeTaskDetailsModal() {
    document.getElementById('taskDetailsModal').classList.add('hidden');
}

// Закрытие модалок при клике вне
window.onclick = function(event) {
    const modalsToCloseOnBgClick = [
        'addMemberModal','leaveTeamModal','createTaskModal',
        'editTaskModal','taskDetailsModal'
    ];
    modalsToCloseOnBgClick.forEach(modalId => {
        const modal = document.getElementById(modalId);
        if (event.target === modal) {
            modal.classList.add('hidden');
        }
    });
};

// Нажимаем кнопку "Удалить задачу" в taskDetailsModal => открываем confirm-окно
function openDeleteTaskModal() {
    // Вставляем taskId в форму удаления
    document.getElementById('deleteTaskId').value = currentTaskId;
    // Открываем окно confirm
    document.getElementById('deleteTaskModal').classList.remove('hidden');
    closeTaskDetailsModal()
}

function closeDeleteTaskModal() {
    document.getElementById('deleteTaskModal').classList.add('hidden');
}

function openCreateSubtaskModal() {
    // мы уже знаем currentTaskId из openTaskDetailsModal
    // (где вы сохранили currentTaskId = ...;)
    // Нужно также узнать teamId (можно получить один раз th:value или global var)

    const teamId = [[${team.id}]]; // если шаблонизатор позволяет
    const modal = document.getElementById('createSubtaskModal');

    // Заполняем скрытые поля:
    document.getElementById('createSubtaskTeamId').value = teamId;
    document.getElementById('createSubtaskTaskId').value = currentTaskId;
    // currentTaskId мы запоминали, когда открывали детали задачи

    modal.classList.remove('hidden');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.add('hidden');
}