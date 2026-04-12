document.addEventListener('DOMContentLoaded', function() {
    const output = document.getElementById('terminal-output');

    const banner = document.getElementById('notification-banner');
    const container = document.getElementById('notification-container');

    const eventSource = new EventSource('/api/events/NOTIFICATIONS');
    const terminalSource = new EventSource('/api/events/TERMINAL');

    const handleSse = (source, targetElement) => {
        let activeElement = targetElement;

        source.addEventListener('start', (e) => {
            const eventType = JSON.parse(e.data);
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
            } else if (source === terminalSource) {
                const block = document.createElement('div');
                block.className = 'terminal-block';
                if (eventType === 'agent-work') {
                    block.classList.add('agent-work');
                }
                targetElement.appendChild(block);
                activeElement = block;
            }
        });

        source.addEventListener('message', (e) => {
            if (activeElement) {
                try {
                    const data = JSON.parse(e.data);
                    activeElement.textContent += data;
                    if (source === terminalSource) targetElement.scrollTop = targetElement.scrollHeight;
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
            if (source === terminalSource) targetElement.textContent = '';
            else if (activeElement) activeElement.textContent = '';
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
            activeElement = null;
        });

        source.onerror = (e) => console.error("SSE Error: ", e);
    };

    handleSse(eventSource, null);
    handleSse(terminalSource, output);

    const btnTest = document.getElementById('btn-test');
    if (btnTest) {
        btnTest.addEventListener('click', async function() {
            try {
                const response = await fetch('/api/agent/run', { method: 'POST' });
                const runId = await response.text();
                console.log('Test Run started:', runId);
            } catch (error) {
                console.error('Error starting test run:', error);
            }
        });
    }

});
