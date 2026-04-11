document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('chat-form');
    const input = document.getElementById('message-input');
    const output = document.getElementById('chat-output');
    const conversationHistory = [];

    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        const message = input.value.trim();
        if (!message) return;

        output.innerHTML += '<div class="user-msg">&gt; You: ' + message + '</div>';
        input.value = '';
        output.innerHTML += '<div class="waiting-msg">&gt; Waiting...</div>';

        try {
            const response = await fetch('/api/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    message: message,
                    history: [...conversationHistory]
                })
            });

            const data = await response.json();
            const waitingMsg = Array.from(output.querySelectorAll('.waiting-msg')).pop();
            if (waitingMsg) waitingMsg.remove();

            if (data.success) {
                output.innerHTML += '<div class="ai-msg">&gt; AI: ' + data.content + '</div>';

                conversationHistory.push({
                    role: 'user',
                    content: message,
                    timestamp: new Date().toISOString()
                });
                conversationHistory.push({
                    role: 'assistant',
                    content: data.content,
                    timestamp: new Date().toISOString()
                });
            } else {
                output.innerHTML += '<div class="error-msg">&gt; Error: ' + data.errorMessage + '</div>';
            }
        } catch (error) {
            const waitingMsg = Array.from(output.querySelectorAll('.waiting-msg')).pop();
            if (waitingMsg) waitingMsg.remove();
            output.innerHTML += '<div class="error-msg">&gt; Error: ' + error.message + '</div>';
        }

        output.scrollTop = output.scrollHeight;
    });

    const banner = document.getElementById('notification-banner');
    const container = document.getElementById('notification-container');
    let currentItem = null;

    const eventSource = new EventSource('/api/events/NOTIFICATIONS');
    const terminalSource = new EventSource('/api/events/TERMINAL');

    const handleSse = (source, targetElement) => {
        let activeElement = targetElement;

        source.addEventListener('start', (e) => {
            if (source === eventSource) {
                banner.style.display = 'flex';
                const item = document.createElement('div');
                item.className = 'notification-item';
                const label = document.createElement('span');
                label.className = 'notification-label';
                label.textContent = 'NOTIFICATION:';
                const text = document.createElement('span');
                text.className = 'notification-text';
                item.appendChild(label);
                item.appendChild(text);
                container.appendChild(item);
                activeElement = text;
            }
        });

        source.addEventListener('message', (e) => {
            if (activeElement) {
                try {
                    activeElement.textContent += JSON.parse(e.data);
                    if (source === terminalSource) activeElement.scrollTop = activeElement.scrollHeight;
                } catch (err) {
                    activeElement.textContent += e.data;
                }
            }
        });

        source.addEventListener('delete', (e) => {
            if (activeElement) {
                try {
                    const data = JSON.parse(e.data);
                    const currentText = activeElement.textContent;
                    if (currentText.endsWith(data)) {
                        activeElement.textContent = currentText.substring(0, currentText.length - data.length);
                    }
                } catch (err) {}
            }
        });

        source.addEventListener('clear', (e) => {
            if (activeElement) activeElement.textContent = '';
        });

        source.addEventListener('end', (e) => {
            if (source === eventSource) {
                const itemToRemove = activeElement ? activeElement.parentElement : null;
                if (itemToRemove) {
                    setTimeout(() => {
                        itemToRemove.remove();
                        if (container.children.length === 0) banner.style.display = 'none';
                    }, 30000);
                }
            }
            if (source === terminalSource) activeElement = targetElement;
            else activeElement = null;
        });

        source.onerror = (e) => console.error("SSE Error: ", e);
    };

    handleSse(eventSource, null);
    handleSse(terminalSource, document.getElementById('chat-output'));

    const btnTest = document.getElementById('btn-test');
    if (btnTest) {
        btnTest.addEventListener('click', async function() {
            try {
                const response = await fetch('/api/run/test', { method: 'POST' });
                const runId = await response.text();
                console.log('Test Run started:', runId);
            } catch (error) {
                console.error('Error starting test run:', error);
            }
        });
    }

});
