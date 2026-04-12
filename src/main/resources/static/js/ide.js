/**
 * MiniDev IDE JavaScript
 */
document.addEventListener('DOMContentLoaded', () => {
    console.log('IDE loaded');
    
    const terminalOutput = document.getElementById('terminal-output');
    const projectList = document.getElementById('project-list');
    const playBtn = document.getElementById('play-project-tab');
    const refreshBtn = document.getElementById('refresh-projects-btn');
    const startAgentBtn = document.getElementById('start-agent-btn');
    let activeTerminalBlock = null;
    let selectedRunId = null;
    
    const editorHtml = document.querySelector('.editor-pane[data-editor="html"] .editor-body');
    const editorCss = document.querySelector('.editor-pane[data-editor="css"] .editor-body');
    const editorJs = document.querySelector('.editor-pane[data-editor="js"] .editor-body');

    // Tab Switching Logic
    const tabButtons = document.querySelectorAll('.tab-button');
    const editorPanes = document.querySelectorAll('.editor-pane');

    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const targetEditor = button.getAttribute('data-editor');
            if (!targetEditor) return; // For non-editor tabs like PLAY

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

    // Refresh Button Event
    if (refreshBtn) {
        refreshBtn.addEventListener('click', () => {
            loadProjects();
        });
    }

    // Start Agent Button Event
    if (startAgentBtn) {
        startAgentBtn.addEventListener('click', () => {
            if (confirm('Möchten Sie einen neuen Agenten-Run starten?')) {
                fetch('/api/agent/run', { method: 'POST' })
                    .then(response => {
                        if (response.ok) {
                            return response.text();
                        }
                        throw new Error('Fehler beim Starten des Agenten');
                    })
                    .then(runId => {
                        console.log('New run started:', runId);
                        loadProjects();
                        // Terminal leeren für den neuen Run
                        terminalOutput.innerHTML = '';
                    })
                    .catch(err => alert(err.message));
            }
        });
    }

    // Play Button Event
    if (playBtn) {
        playBtn.addEventListener('click', () => {
            if (selectedRunId) {
                window.open(`/games-static/run-${selectedRunId}`, '_blank');
            }
        });
    }

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
            const { fileType, char, position } = data;
            const editor = getEditor(fileType);
            if (editor) {
                if (position !== undefined) {
                    const text = editor.textContent;
                    editor.textContent = text.slice(0, position) + char + text.slice(position);
                } else {
                    editor.textContent += char;
                }
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
        try {
            const data = JSON.parse(e.data);
            const fileType = typeof data === 'string' ? data : data.fileType;
            const position = typeof data === 'object' ? data.position : undefined;
            
            const editor = getEditor(fileType);
            if (editor) {
                const text = editor.textContent;
                if (text.length > 0) {
                    if (position !== undefined) {
                        editor.textContent = text.slice(0, position) + text.slice(position + 1);
                    } else {
                        editor.textContent = text.substring(0, text.length - 1);
                    }
                }
            }
        } catch (err) {
            console.error("Error processing file-delete", err);
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

                if (selectedRunId === runId) {
                    item.classList.add('active');
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
        selectedRunId = runId;
        if (playBtn) playBtn.disabled = false;
        
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

});
