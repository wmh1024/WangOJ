import ace from 'ace-builds'

import themeGithubUrl from 'ace-builds/src-noconflict/theme-github?url'
import themeChromeUrl from 'ace-builds/src-noconflict/theme-chrome?url'
import themeMonokaiUrl from 'ace-builds/src-noconflict/theme-monokai?url'
import workerBaseUrl from 'ace-builds/src-noconflict/worker-base?url'
import extSearchboxUrl from 'ace-builds/src-noconflict/ext-searchbox?url'
import 'ace-builds/src-noconflict/ext-language_tools'

import modeCPPUrl from 'ace-builds/src-noconflict/mode-c_cpp?url'
import snippetsCPPUrl from 'ace-builds/src-noconflict/snippets/c_cpp?url'
import modeJavaUrl from 'ace-builds/src-noconflict/mode-java?url'
import snippetsJavaUrl from 'ace-builds/src-noconflict/snippets/java?url'
import modePythonUrl from 'ace-builds/src-noconflict/mode-python?url'
import snippetsPythonUrl from 'ace-builds/src-noconflict/snippets/python?url'
import modeJavascriptUrl from 'ace-builds/src-noconflict/mode-javascript?url'
import snippetsJavascriptUrl from 'ace-builds/src-noconflict/snippets/javascript?url'
import modeGolangUrl from 'ace-builds/src-noconflict/mode-golang?url'
import snippetsGolangUrl from 'ace-builds/src-noconflict/snippets/golang?url'

ace.config.setModuleUrl('ace/theme/github', themeGithubUrl)
ace.config.setModuleUrl('ace/theme/chrome', themeChromeUrl)
ace.config.setModuleUrl('ace/theme/monokai', themeMonokaiUrl)

ace.config.setModuleUrl('ace/mode/c_cpp', modeCPPUrl)
ace.config.setModuleUrl('ace/mode/java', modeJavaUrl)
ace.config.setModuleUrl('ace/mode/python', modePythonUrl)
ace.config.setModuleUrl('ace/mode/javascript', modeJavascriptUrl)
ace.config.setModuleUrl('ace/mode/golang', modeGolangUrl)

ace.config.setModuleUrl('ace/mode/base', workerBaseUrl)

ace.config.setModuleUrl('ace/snippets/c_cpp', snippetsCPPUrl)
ace.config.setModuleUrl('ace/snippets/java', snippetsJavaUrl)
ace.config.setModuleUrl('ace/snippets/python', snippetsPythonUrl)
ace.config.setModuleUrl('ace/snippets/javascript', snippetsJavascriptUrl)
ace.config.setModuleUrl('ace/snippets/golang', snippetsGolangUrl)

ace.config.setModuleUrl('ace/ext/searchbox', extSearchboxUrl)

ace.require('ace/ext/language_tools')
