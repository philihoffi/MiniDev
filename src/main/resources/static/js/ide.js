/**
 * MiniDev IDE JavaScript
 */
document.addEventListener('DOMContentLoaded', () => {
    console.log('IDE loaded');
    
    const terminalOutput = document.getElementById('terminal-output');
    const projectList = document.getElementById('project-list');
    let activeTerminalBlock = null;
    
    const editorHtml = document.querySelector('.editor-pane[data-editor="html"] .editor-body');
    const editorCss = document.querySelector('.editor-pane[data-editor="css"] .editor-body');
    const editorJs = document.querySelector('.editor-pane[data-editor="js"] .editor-body');

    // Tab Switching Logic
    const tabButtons = document.querySelectorAll('.tab-button');
    const editorPanes = document.querySelectorAll('.editor-pane');

    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const targetEditor = button.getAttribute('data-editor');
            
            // Update buttons
            tabButtons.forEach(btn => btn.classList.remove('active'));
            button.classList.add('active');
            
            // Update panes
            editorPanes.forEach(pane => {
                if (pane.getAttribute('data-editor') === targetEditor) {
                    pane.classList.add('active');
                } else {
                    pane.classList.remove('active');
                }
            });
            
            console.log(`Switched to editor: ${targetEditor}`);
        });
    });

    const terminalSource = new EventSource('/api/events/TERMINAL');
    
    terminalSource.addEventListener('start', (e) => {
        const eventType = JSON.parse(e.data);
        const block = document.createElement('div');
        block.className = 'terminal-block';
        if (eventType === 'agent-work') {
            block.classList.add('agent-work');
        }
        terminalOutput.appendChild(block);
        activeTerminalBlock = block;
    });

    terminalSource.addEventListener('message', (e) => {
        let data;
        try {
            data = JSON.parse(e.data);
        } catch (err) {
            data = e.data;
        }

        if (activeTerminalBlock) {
            activeTerminalBlock.textContent += data;
            terminalOutput.scrollTop = terminalOutput.scrollHeight;
        } else {
            logToTerminal(data);
        }
    });

    terminalSource.addEventListener('end', () => {
        activeTerminalBlock = null;
    });

    terminalSource.addEventListener('clear', () => {
        terminalOutput.textContent = '';
    });

    window.addEventListener('beforeunload', () => {
        console.log('Closing IDE SSE connection...');
        terminalSource.close();
    });

    // Einfaches Logging ins "Terminal"
    function logToTerminal(message, type = 'info') {
        const line = document.createElement('div');
        line.className = `terminal-line ${type}`;
        line.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
        terminalOutput.appendChild(line);
        terminalOutput.scrollTop = terminalOutput.scrollHeight;
    }

    async function loadProjects() {
        try {
            const response = await fetch('/api/agent/games');
            const games = await response.json();
            
            projectList.innerHTML = '';
            games.forEach(game => {
                const item = document.createElement('div');
                item.className = 'project-item';
                // GameMetadata hat ein runId Feld (UUID)
                const runId = game.runId;
                item.textContent = game.name || runId;
                item.title = runId;
                item.addEventListener('click', () => loadProjectContent(runId, item));
                projectList.appendChild(item);
            });
            logToTerminal(`${games.length} Projekte geladen.`);
        } catch (error) {
            logToTerminal('Fehler beim Laden der Projekte: ' + error.message, 'error');
        }
    }

    async function loadProjectContent(runId, element) {
        document.querySelectorAll('.project-item').forEach(i => i.classList.remove('active'));
        element.classList.add('active');
        
        logToTerminal(`Lade Projekt ${element.textContent}...`);
        
        try {
            const response = await fetch(`/api/agent/games/${runId}/components`);
            if (!response.ok) throw new Error('Projekt nicht gefunden');
            
            const data = await response.json();
            
            editorHtml.textContent = data.html || '';
            editorCss.textContent = data.css || '';
            editorJs.textContent = data.js || '';
            
            logToTerminal(`Projekt ${element.textContent} erfolgreich geladen.`);
        } catch (error) {
            logToTerminal(`Fehler: ${error.message}`, 'error');
        }
    }

    // Initiales Laden
    loadProjects();
    logToTerminal('System bereit.');
});
