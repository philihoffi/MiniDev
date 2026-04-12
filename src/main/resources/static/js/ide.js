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
    const ideSource = new EventSource('/api/events/IDE');

    function getEditor(fileType) {
        if (fileType === 'html') return editorHtml;
        if (fileType === 'css') return editorCss;
        if (fileType === 'js') return editorJs;
        return null;
    }

    function switchTab(editorName) {
        const button = document.querySelector(`.tab-button[data-editor="${editorName}"]`);
        if (button) {
            button.click();
        }
    }

    ideSource.addEventListener('switch-tab', (e) => {
        const fileType = JSON.parse(e.data);
        switchTab(fileType);
        
        // Aktives Projekt in der Liste markieren, wenn es gerade bearbeitet wird
        const activeProject = document.querySelector('.project-item.working-on');
        if (activeProject && !activeProject.classList.contains('active')) {
            document.querySelectorAll('.project-item').forEach(i => i.classList.remove('active'));
            activeProject.classList.add('active');
        }
    });

    ideSource.addEventListener('file-append', (e) => {
        try {
            const data = JSON.parse(e.data);
            const { fileType, char } = data;
            const editor = getEditor(fileType);
            if (editor) {
                editor.textContent += char;
                const container = editor.closest('.editor-body');
                if (container) {
                    container.scrollTop = container.scrollHeight;
                }
            }
        } catch (err) {
            console.error("Error processing file-append", err);
        }
    });

    ideSource.addEventListener('file-delete', (e) => {
        const fileType = JSON.parse(e.data);
        const editor = getEditor(fileType);
        if (editor) {
            const text = editor.textContent;
            if (text.length > 0) {
                editor.textContent = text.substring(0, text.length - 1);
            }
        }
    });

    ideSource.addEventListener('message', (e) => {
        // Dieser Listener wird nur für das initiale Laden oder explizite Updates verwendet, 
        // wenn KEIN Streaming stattfindet.
        try {
            const data = JSON.parse(e.data);
            const { fileType, content } = data;
            const editor = getEditor(fileType);

            if (editor) {
                // Nur aktualisieren, wenn nicht gerade ein Stream läuft oder wir sicher sind, 
                // dass wir den ganzen Inhalt brauchen (z.B. nach Review)
                console.log(`Received full content update for ${fileType}`);
                editor.textContent = content;
                const container = editor.closest('.editor-body');
                if (container) {
                    container.scrollTop = container.scrollHeight;
                }
            }
        } catch (err) {
            console.error("Error processing IDE message event", err);
        }
    });
    
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
        loadProjects(); // Projektliste aktualisieren, wenn ein Run fertig ist
    });

    terminalSource.addEventListener('clear', () => {
        terminalOutput.textContent = '';
    });

    window.addEventListener('beforeunload', () => {
        console.log('Closing IDE SSE connections...');
        terminalSource.close();
        ideSource.close();
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
            const [gamesResponse, activeRunResponse] = await Promise.all([
                fetch('/api/agent/games'),
                fetch('/api/agent/active-run')
            ]);
            
            const games = await gamesResponse.json();
            let activeRunId = null;
            if (activeRunResponse.status === 200) {
                activeRunId = await activeRunResponse.json();
            }
            
            projectList.innerHTML = '';
            games.forEach(game => {
                const item = document.createElement('div');
                item.className = 'project-item';
                // GameMetadata hat ein runId Feld (UUID)
                const runId = game.runId;
                item.textContent = game.name || runId;
                item.title = runId;
                
                if (runId === activeRunId) {
                    item.classList.add('working-on');
                    item.title += " (In Arbeit)";
                }

                item.addEventListener('click', () => loadProjectContent(runId, item));
                projectList.appendChild(item);
            });
            // logToTerminal(`${games.length} Projekte geladen.`);
        } catch (error) {
            // logToTerminal('Fehler beim Laden der Projekte: ' + error.message, 'error');
        }
    }

    async function loadProjectContent(runId, element) {
        document.querySelectorAll('.project-item').forEach(i => i.classList.remove('active'));
        element.classList.add('active');
        
        // logToTerminal(`Lade Projekt ${element.textContent}...`);
        
        // Editor-Inhalte vor dem Laden leeren
        editorHtml.textContent = '';
        editorCss.textContent = '';
        editorJs.textContent = '';
        
        try {
            const response = await fetch(`/api/agent/games/${runId}/components`);
            if (!response.ok) throw new Error('Projekt nicht gefunden');
            
            const data = await response.json();
            
            editorHtml.textContent = data.html || '';
            editorCss.textContent = data.css || '';
            editorJs.textContent = data.js || '';
            
            // logToTerminal(`Projekt ${element.textContent} erfolgreich geladen.`);
        } catch (error) {
            // logToTerminal(`Fehler: ${error.message}`, 'error');
        }
    }

    // Initiales Laden
    loadProjects();
    // logToTerminal('System bereit.');

    // Refresh Button Event
    const refreshBtn = document.querySelector('.action-row .action-button');
    if (refreshBtn && refreshBtn.textContent === 'Refresh') {
        refreshBtn.addEventListener('click', () => {
            // logToTerminal('Aktualisiere Projektliste...');
            loadProjects();
        });
    }
});
