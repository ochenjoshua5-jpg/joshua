/*
 * OJ MUSIC - Windows launcher
 * Made by Ochen Joshua
 *
 * This is a small native Win32 application that starts the full OJ MUSIC
 * Android app on a connected phone / tablet or on an Android emulator.
 *
 * The Android package (APK) is appended to the end of this executable by
 * launcher/build_launcher.py, so the .exe is fully self contained:
 *
 *      [ launcher.exe ][ apk bytes ][ 8 byte size ][ "OJMUSIC1" ]
 *
 * When started the launcher:
 *   1. extracts the bundled APK to %LOCALAPPDATA%\OJ MUSIC\OJ-MUSIC.apk
 *   2. locates adb (next to the exe, Android SDK, or PATH)
 *   3. waits for a device - starting an emulator when none is running
 *   4. installs and launches com.ochenjoshua.ojmusicplayer
 *
 * Build (any mingw-w64 compatible compiler, e.g. zig cc):
 *   zig cc -target x86_64-windows-gnu -O2 -mwindows ojmusic_launcher.c -o OJMUSIC.exe -lole32 -lshell32
 */

#define UNICODE
#define _UNICODE

#include <windows.h>
#include <shellapi.h>
#include <shlobj.h>
#include <commdlg.h>
#include <string.h>
#include <stdio.h>

#define ID_LOG        1001
#define ID_BTN_GO     1002
#define ID_BTN_SAVE   1003
#define ID_BTN_FOLDER 1004
#define ID_STATUS     1005
#define WM_APP_LOG    (WM_APP + 1)

#define FOOTER_MAGIC  "OJMUSIC1"
#define FOOTER_LEN    16
#define APK_RES_NAME  L"OJ-MUSIC.apk"
#define APP_PACKAGE   L"com.ochenjoshua.ojmusicplayer"
#define APP_ACTIVITY  L"com.ochenjoshua.ojmusicplayer/.MainActivity"

static HWND     g_hwnd, g_log, g_status, g_btnGo, g_btnSave, g_btnFolder;
static wchar_t  g_exeDir[MAX_PATH];
static wchar_t  g_apkPath[MAX_PATH];
static wchar_t  g_logBuf[512 * 1024];
static CRITICAL_SECTION g_logLock;
static volatile LONG g_busy = 0;

/* ------------------------------------------------------------------ utils */

static BOOL file_exists(const wchar_t *p)
{
    DWORD a = GetFileAttributesW(p);
    return (a != INVALID_FILE_ATTRIBUTES) && !(a & FILE_ATTRIBUTE_DIRECTORY);
}

static BOOL dir_exists(const wchar_t *p)
{
    DWORD a = GetFileAttributesW(p);
    return (a != INVALID_FILE_ATTRIBUTES) && (a & FILE_ATTRIBUTE_DIRECTORY);
}

static void join_path(wchar_t *out, size_t cch, const wchar_t *a, const wchar_t *b)
{
    lstrcpynW(out, a, (int)cch);
    if (out[0] && out[lstrlenW(out) - 1] != L'\\')
        lstrcatW(out, L"\\");
    lstrcatW(out, b);
}

static void log_line(const wchar_t *msg)
{
    EnterCriticalSection(&g_logLock);
    size_t used = lstrlenW(g_logBuf);
    size_t room = (sizeof(g_logBuf) / sizeof(wchar_t)) - used - 4;
    if (room > 4) {
        lstrcpynW(g_logBuf + used, msg, (int)room);
        lstrcatW(g_logBuf, L"\r\n");
    }
    LeaveCriticalSection(&g_logLock);
    PostMessageW(g_hwnd, WM_APP_LOG, 0, 0);
}

static void log_text(const wchar_t *msg) { log_line(msg); }

static void log_num(const wchar_t *label, unsigned long value)
{
    wchar_t b[256];
    wsprintfW(b, L"%s%lu", label, value);
    log_line(b);
}

/* ------------------------------------------------------------------ embed */

static HANDLE open_self(void)
{
    wchar_t self[MAX_PATH * 2];
    GetModuleFileNameW(NULL, self, MAX_PATH * 2);
    return CreateFileW(self, GENERIC_READ, FILE_SHARE_READ, NULL,
                       OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
}

/* Returns the size of the APK embedded in this executable, or 0 on failure. */
static unsigned long long embedded_apk_size(void)
{
    HANDLE h = open_self();
    if (h == INVALID_HANDLE_VALUE) return 0;

    LARGE_INTEGER li, pos;
    unsigned char footer[FOOTER_LEN];
    DWORD got = 0;
    unsigned long long size = 0;
    BOOL ok = FALSE;

    if (GetFileSizeEx(h, &li) && li.QuadPart > FOOTER_LEN) {
        pos.QuadPart = li.QuadPart - FOOTER_LEN;
        SetFilePointerEx(h, pos, NULL, FILE_BEGIN);
        if (ReadFile(h, footer, FOOTER_LEN, &got, NULL) && got == FOOTER_LEN &&
            memcmp(footer + 8, FOOTER_MAGIC, 8) == 0) {
            for (int i = 7; i >= 0; --i) size = (size << 8) | footer[i];
            ok = (size > 0 && size <= (unsigned long long)(li.QuadPart - FOOTER_LEN));
        }
    }
    CloseHandle(h);
    return ok ? size : 0;
}

static BOOL read_embedded_apk(unsigned char **data, DWORD *size)
{
    HANDLE h = open_self();
    if (h == INVALID_HANDLE_VALUE) { log_text(L"[!] Cannot open launcher executable."); return FALSE; }

    LARGE_INTEGER li;
    if (!GetFileSizeEx(h, &li) || li.QuadPart < FOOTER_LEN) {
        log_text(L"[!] The launcher has no bundled APK (footer missing).");
        CloseHandle(h); return FALSE;
    }

    LARGE_INTEGER pos; pos.QuadPart = li.QuadPart - FOOTER_LEN;
    SetFilePointerEx(h, pos, NULL, FILE_BEGIN);

    unsigned char footer[FOOTER_LEN];
    DWORD got = 0;
    if (!ReadFile(h, footer, FOOTER_LEN, &got, NULL) || got != FOOTER_LEN ||
        memcmp(footer + 8, FOOTER_MAGIC, 8) != 0) {
        log_text(L"[!] The launcher has no bundled APK (footer invalid).");
        CloseHandle(h); return FALSE;
    }

    unsigned long long apkSize = 0;
    for (int i = 7; i >= 0; --i) apkSize = (apkSize << 8) | footer[i];
    if (apkSize == 0 || apkSize > (unsigned long long)(li.QuadPart - FOOTER_LEN)) {
        log_text(L"[!] Bundled APK size looks invalid.");
        CloseHandle(h); return FALSE;
    }

    unsigned char *buf = (unsigned char *)HeapAlloc(GetProcessHeap(), 0, (SIZE_T)apkSize);
    if (!buf) { CloseHandle(h); return FALSE; }

    LARGE_INTEGER start; start.QuadPart = li.QuadPart - FOOTER_LEN - (LONGLONG)apkSize;
    SetFilePointerEx(h, start, NULL, FILE_BEGIN);

    DWORD total = 0;
    while (total < (DWORD)apkSize) {
        DWORD chunk = 0;
        if (!ReadFile(h, buf + total, (DWORD)apkSize - total, &chunk, NULL) || chunk == 0) break;
        total += chunk;
    }
    CloseHandle(h);
    if (total != (DWORD)apkSize) { HeapFree(GetProcessHeap(), 0, buf); return FALSE; }

    *data = buf; *size = (DWORD)apkSize;
    return TRUE;
}

static BOOL extract_apk(void)
{
    wchar_t dir[MAX_PATH];
    if (!SUCCEEDED(SHGetFolderPathW(NULL, CSIDL_LOCAL_APPDATA, NULL, 0, dir))) {
        log_text(L"[!] Cannot resolve %LOCALAPPDATA%.");
        return FALSE;
    }
    wchar_t appDir[MAX_PATH];
    join_path(appDir, MAX_PATH, dir, L"OJ MUSIC");
    if (!dir_exists(appDir)) CreateDirectoryW(appDir, NULL);
    join_path(g_apkPath, MAX_PATH, appDir, APK_RES_NAME);

    if (file_exists(g_apkPath)) {
        unsigned long long embedded = embedded_apk_size();
        HANDLE probe = CreateFileW(g_apkPath, GENERIC_READ, FILE_SHARE_READ, NULL,
                                   OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
        LARGE_INTEGER have; have.QuadPart = -1;
        if (probe != INVALID_HANDLE_VALUE) { GetFileSizeEx(probe, &have); CloseHandle(probe); }
        if (embedded != 0 && (unsigned long long)have.QuadPart == embedded) {
            log_text(L"[i] Bundled app already extracted:");
            log_text(g_apkPath);
            return TRUE;
        }
        log_text(L"[i] Refreshing the extracted app package...");
    }

    unsigned char *data = NULL; DWORD size = 0;
    if (!read_embedded_apk(&data, &size)) return FALSE;

    HANDLE out = CreateFileW(g_apkPath, GENERIC_WRITE, 0, NULL, CREATE_ALWAYS,
                             FILE_ATTRIBUTE_NORMAL, NULL);
    if (out == INVALID_HANDLE_VALUE) {
        HeapFree(GetProcessHeap(), 0, data);
        log_text(L"[!] Cannot write the bundled APK (access denied).");
        return FALSE;
    }
    DWORD written = 0;
    BOOL ok = WriteFile(out, data, size, &written, NULL);
    CloseHandle(out);
    HeapFree(GetProcessHeap(), 0, data);

    if (!ok || written != size) { log_text(L"[!] Failed while writing the bundled APK."); return FALSE; }

    log_text(L"[OK] Extracted the OJ MUSIC app package:");
    log_text(g_apkPath);
    return TRUE;
}

/* ------------------------------------------------------------------ adb */

static BOOL find_adb(wchar_t *out)
{
    wchar_t c[MAX_PATH];

    join_path(c, MAX_PATH, g_exeDir, L"adb.exe");
    if (file_exists(c)) { lstrcpyW(out, c); return TRUE; }
    join_path(c, MAX_PATH, g_exeDir, L"platform-tools\\adb.exe");
    if (file_exists(c)) { lstrcpyW(out, c); return TRUE; }

    const wchar_t *envs[] = { L"LOCALAPPDATA", L"USERPROFILE", L"ProgramFiles", L"ProgramFiles(x86)" };
    for (int i = 0; i < 4; ++i) {
        DWORD n = GetEnvironmentVariableW(envs[i], c, MAX_PATH);
        if (n == 0 || n >= MAX_PATH) continue;
        wchar_t p[MAX_PATH];
        join_path(p, MAX_PATH, c, L"Android\\Sdk\\platform-tools\\adb.exe");
        if (file_exists(p)) { lstrcpyW(out, p); return TRUE; }
        join_path(p, MAX_PATH, c, L"AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe");
        if (file_exists(p)) { lstrcpyW(out, p); return TRUE; }
    }

    /* last resort - rely on PATH via CreateProcess */
    lstrcpyW(out, L"adb.exe");
    return FALSE;
}

static BOOL find_emulator(wchar_t *out)
{
    wchar_t c[MAX_PATH];
    join_path(c, MAX_PATH, g_exeDir, L"emulator.exe");
    if (file_exists(c)) { lstrcpyW(out, c); return TRUE; }

    const wchar_t *envs[] = { L"LOCALAPPDATA", L"USERPROFILE" };
    for (int i = 0; i < 2; ++i) {
        DWORD n = GetEnvironmentVariableW(envs[i], c, MAX_PATH);
        if (n == 0 || n >= MAX_PATH) continue;
        wchar_t p[MAX_PATH];
        join_path(p, MAX_PATH, c, L"Android\\Sdk\\emulator\\emulator.exe");
        if (file_exists(p)) { lstrcpyW(out, p); return TRUE; }
        join_path(p, MAX_PATH, c, L"AppData\\Local\\Android\\Sdk\\emulator\\emulator.exe");
        if (file_exists(p)) { lstrcpyW(out, p); return TRUE; }
    }
    return FALSE;
}

/* Runs a command, streams its output into the log, returns exit code (-1 = fail) */
static int run_capture(const wchar_t *cmdline, DWORD timeout_ms)
{
    wchar_t mut[MAX_PATH * 4];
    lstrcpynW(mut, cmdline, MAX_PATH * 4);

    SECURITY_ATTRIBUTES sa;
    ZeroMemory(&sa, sizeof(sa));
    sa.nLength = sizeof(sa);
    sa.bInheritHandle = TRUE;

    HANDLE rd = NULL, wr = NULL;
    if (!CreatePipe(&rd, &wr, &sa, 0)) return -1;
    SetHandleInformation(rd, HANDLE_FLAG_INHERIT, 0);

    STARTUPINFOW si;
    ZeroMemory(&si, sizeof(si));
    si.cb = sizeof(si);
    si.dwFlags = STARTF_USESTDHANDLES | STARTF_USESHOWWINDOW;
    si.wShowWindow = SW_HIDE;
    si.hStdOutput = wr;
    si.hStdError = wr;

    PROCESS_INFORMATION pi;
    ZeroMemory(&pi, sizeof(pi));

    BOOL started = CreateProcessW(NULL, mut, NULL, NULL, TRUE,
                                  CREATE_NO_WINDOW, NULL, NULL, &si, &pi);
    CloseHandle(wr);
    if (!started) { CloseHandle(rd); return -1; }

    char buf[4096];
    DWORD got = 0;
    DWORD start = GetTickCount();
    for (;;) {
        DWORD avail = 0;
        if (PeekNamedPipe(rd, NULL, 0, NULL, &avail, NULL) && avail > 0) {
            DWORD want = avail > sizeof(buf) - 1 ? sizeof(buf) - 1 : avail;
            if (ReadFile(rd, buf, want, &got, NULL) && got > 0) {
                buf[got] = 0;
                wchar_t wbuf[4096];
                MultiByteToWideChar(CP_UTF8, 0, buf, -1, wbuf, 4096);
                if (wbuf[0]) log_line(wbuf);
            }
            continue;
        }
        DWORD st = WaitForSingleObject(pi.hProcess, 40);
        if (st == WAIT_OBJECT_0) {
            while (PeekNamedPipe(rd, NULL, 0, NULL, &avail, NULL) && avail > 0) {
                DWORD want = avail > sizeof(buf) - 1 ? sizeof(buf) - 1 : avail;
                if (ReadFile(rd, buf, want, &got, NULL) && got > 0) {
                    buf[got] = 0;
                    wchar_t wbuf[4096];
                    MultiByteToWideChar(CP_UTF8, 0, buf, -1, wbuf, 4096);
                    if (wbuf[0]) log_line(wbuf);
                }
            }
            break;
        }
        if (GetTickCount() - start > timeout_ms) {
            log_text(L"[!] Command timed out.");
            TerminateProcess(pi.hProcess, 1);
            break;
        }
        Sleep(20);
    }

    DWORD code = 1;
    GetExitCodeProcess(pi.hProcess, &code);
    CloseHandle(pi.hProcess);
    CloseHandle(pi.hThread);
    CloseHandle(rd);
    return (int)code;
}

static BOOL device_connected(const wchar_t *adb)
{
    wchar_t cmd[MAX_PATH * 3];
    wchar_t tmp[MAX_PATH];
    GetTempPathW(MAX_PATH, tmp);
    wchar_t file[MAX_PATH];
    join_path(file, MAX_PATH, tmp, L"ojmusic_devices.txt");
    wsprintfW(cmd, L"\"%s\" devices", adb);

    /* simple approach: run and inspect the log buffer tail is unreliable,
       so run "adb devices" through a file */
    wchar_t full[MAX_PATH * 4];
    wsprintfW(full, L"cmd.exe /c \"%s\" > \"%s\" 2>&1", cmd, file);
    run_capture(full, 20000);

    HANDLE h = CreateFileW(file, GENERIC_READ, FILE_SHARE_READ, NULL,
                           OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (h == INVALID_HANDLE_VALUE) return FALSE;
    char data[8192];
    DWORD got = 0;
    ReadFile(h, data, sizeof(data) - 1, &got, NULL);
    CloseHandle(h);
    data[got] = 0;
    DeleteFileW(file);

    /* device lines look like "SERIAL\tdevice" */
    char *line = strtok(data, "\r\n");
    BOOL found = FALSE;
    while (line) {
        if (strstr(line, "\tdevice") && !strstr(line, "List of devices")) found = TRUE;
        line = strtok(NULL, "\r\n");
    }
    return found;
}

static BOOL start_emulator_if_available(const wchar_t *adb)
{
    wchar_t emu[MAX_PATH];
    if (!find_emulator(emu)) return FALSE;

    log_text(L"[i] No phone detected - looking for an Android emulator...");
    wchar_t tmp[MAX_PATH], avdFile[MAX_PATH];
    GetTempPathW(MAX_PATH, tmp);
    join_path(avdFile, MAX_PATH, tmp, L"ojmusic_avds.txt");

    wchar_t cmd[MAX_PATH * 4];
    wsprintfW(cmd, L"cmd.exe /c \"\"%s\" -list-avds\" > \"%s\" 2>&1", emu, avdFile);
    run_capture(cmd, 30000);

    HANDLE h = CreateFileW(avdFile, GENERIC_READ, FILE_SHARE_READ, NULL,
                           OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);
    if (h == INVALID_HANDLE_VALUE) return FALSE;
    char data[8192];
    DWORD got = 0;
    ReadFile(h, data, sizeof(data) - 1, &got, NULL);
    CloseHandle(h);
    data[got] = 0;
    DeleteFileW(avdFile);

    char *nl = strpbrk(data, "\r\n");
    if (nl) *nl = 0;
    if (data[0] == 0) return FALSE;

    wchar_t avd[512];
    MultiByteToWideChar(CP_UTF8, 0, data, -1, avd, 512);

    log_text(L"[i] Starting emulator: ");
    log_text(avd);
    wchar_t start[MAX_PATH * 4];
    wsprintfW(start, L"cmd.exe /c start \"\" \"%s\" -avd %s -no-snapshot-load", emu, avd);
    run_capture(start, 20000);

    log_text(L"[i] Waiting for the emulator to boot (this can take a minute)...");
    wchar_t wait[MAX_PATH * 4];
    wsprintfW(wait, L"\"%s\" wait-for-device", adb);
    run_capture(wait, 240000);
    return TRUE;
}

/* ------------------------------------------------------------------ worker */

static DWORD WINAPI worker(LPVOID param)
{
    (void)param;
    wchar_t adb[MAX_PATH];
    BOOL haveAdb = find_adb(adb);

    log_text(L"==============================================");
    log_text(L"  OJ MUSIC - Made by Ochen Joshua");
    log_text(L"==============================================");

    if (!extract_apk()) goto done;

    if (haveAdb) {
        log_text(L"[OK] Android platform tools found:");
        log_text(adb);
    } else {
        log_text(L"[!] adb.exe was not found on this PC.");
        log_text(L"    Install Android Platform Tools, or copy the APK to your phone");
        log_text(L"    and install it manually - use the 'Save APK' button below.");
    }

    if (haveAdb) {
        wchar_t cmd[MAX_PATH * 4];
        wsprintfW(cmd, L"\"%s\" start-server", adb);
        run_capture(cmd, 60000);

        if (!device_connected(adb)) {
            start_emulator_if_available(adb);
            if (!device_connected(adb)) {
                log_text(L"[!] No phone or emulator is connected.");
                log_text(L"    Connect your phone with USB debugging enabled, then press");
                log_text(L"    'Install & Launch' again.");
                goto done;
            }
        }

        log_text(L"[i] Installing OJ MUSIC on your device...");
        wchar_t inst[MAX_PATH * 4];
        wsprintfW(inst, L"\"%s\" install -r -d \"%s\"", adb, g_apkPath);
        int rc = run_capture(inst, 300000);
        if (rc != 0) {
            log_text(L"[!] The install reported an error (see the log above).");
            log_text(L"    If the app is already installed, try uninstalling it first:");
            log_text(L"    adb uninstall " APP_PACKAGE);
            goto done;
        }

        log_text(L"[OK] Installed. Starting OJ MUSIC...");
        wchar_t go[MAX_PATH * 4];
        wsprintfW(go, L"\"%s\" shell am start -n " APP_ACTIVITY, adb);
        run_capture(go, 60000);

        log_text(L"[OK] OJ MUSIC is running on your device. Enjoy the music!");
    }

done:
    log_text(L"----------------------------------------------");
    InterlockedExchange(&g_busy, 0);
    PostMessageW(g_hwnd, WM_APP_LOG, 1, 0);
    return 0;
}

/* ------------------------------------------------------------------ ui */

static void set_status(const wchar_t *s) { SetWindowTextW(g_status, s); }

static void refresh_log(void)
{
    EnterCriticalSection(&g_logLock);
    SetWindowTextW(g_log, g_logBuf);
    LeaveCriticalSection(&g_logLock);
    SendMessageW(g_log, EM_SETSEL, (WPARAM)-1, (LPARAM)-1);
    SendMessageW(g_log, EM_SCROLLCARET, 0, 0);
}

static void save_apk_dialog(void)
{
    if (g_apkPath[0] == 0) {
        if (!extract_apk()) return;
    }
    wchar_t target[MAX_PATH];
    lstrcpyW(target, APK_RES_NAME);
    OPENFILENAMEW ofn;
    ZeroMemory(&ofn, sizeof(ofn));
    ofn.lStructSize = sizeof(ofn);
    ofn.hwndOwner = g_hwnd;
    ofn.lpstrFilter = L"Android package (*.apk)\0*.apk\0All files (*.*)\0*.*\0";
    ofn.lpstrFile = target;
    ofn.nMaxFile = MAX_PATH;
    ofn.lpstrDefExt = L"apk";
    ofn.Flags = OFN_OVERWRITEPROMPT | OFN_PATHMUSTEXIST;
    if (GetSaveFileNameW(&ofn)) {
        if (CopyFileW(g_apkPath, target, FALSE)) {
            MessageBoxW(g_hwnd, L"The OJ MUSIC package was saved.\nCopy it to your phone and tap it to install.",
                        L"OJ MUSIC", MB_OK | MB_ICONINFORMATION);
        } else {
            MessageBoxW(g_hwnd, L"Could not save the file.", L"OJ MUSIC", MB_OK | MB_ICONERROR);
        }
    }
}

static void open_apk_folder(void)
{
    if (g_apkPath[0] == 0) { if (!extract_apk()) return; }
    wchar_t params[MAX_PATH + 32];
    wsprintfW(params, L"/select,\"%s\"", g_apkPath);
    ShellExecuteW(NULL, L"open", L"explorer.exe", params, NULL, SW_SHOWNORMAL);
}

static LRESULT CALLBACK wnd_proc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp)
{
    switch (msg) {
    case WM_CREATE:
        g_hwnd = hwnd;
        g_status = CreateWindowW(L"STATIC", L"Ready. Press 'Install && Launch' to start OJ MUSIC on your device.",
                                 WS_CHILD | WS_VISIBLE | SS_LEFT,
                                 16, 12, 760, 22, hwnd, (HMENU)ID_STATUS, NULL, NULL);
        g_btnGo = CreateWindowW(L"BUTTON", L"Install && Launch",
                                WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON,
                                16, 44, 190, 38, hwnd, (HMENU)ID_BTN_GO, NULL, NULL);
        g_btnSave = CreateWindowW(L"BUTTON", L"Save APK...",
                                  WS_CHILD | WS_VISIBLE,
                                  216, 44, 150, 38, hwnd, (HMENU)ID_BTN_SAVE, NULL, NULL);
        g_btnFolder = CreateWindowW(L"BUTTON", L"Open APK location",
                                    WS_CHILD | WS_VISIBLE,
                                    376, 44, 170, 38, hwnd, (HMENU)ID_BTN_FOLDER, NULL, NULL);
        g_log = CreateWindowW(L"EDIT", L"",
                              WS_CHILD | WS_VISIBLE | WS_VSCROLL | ES_LEFT | ES_MULTILINE |
                              ES_AUTOVSCROLL | ES_READONLY,
                              16, 96, 760, 400, hwnd, (HMENU)ID_LOG, NULL, NULL);
        SendMessageW(g_log, WM_SETFONT, (WPARAM)GetStockObject(ANSI_FIXED_FONT), TRUE);
        return 0;

    case WM_SIZE: {
        int w = LOWORD(lp), h = HIWORD(lp);
        SetWindowPos(g_log, NULL, 16, 96, w - 32, h - 112, SWP_NOZORDER);
        return 0;
    }

    case WM_APP_LOG:
        refresh_log();
        if (wp == 1) {
            set_status(g_busy ? L"Working..." : L"Done. OJ MUSIC is ready.");
            if (!g_busy) {
                EnableWindow(g_btnGo, TRUE);
                EnableWindow(g_btnSave, TRUE);
                EnableWindow(g_btnFolder, TRUE);
            }
        }
        return 0;

    case WM_COMMAND:
        switch (LOWORD(wp)) {
        case ID_BTN_GO:
            if (InterlockedCompareExchange(&g_busy, 1, 0) == 0) {
                EnableWindow(g_btnGo, FALSE);
                set_status(L"Working - please wait...");
                CloseHandle(CreateThread(NULL, 0, worker, NULL, 0, NULL));
            }
            return 0;
        case ID_BTN_SAVE:   save_apk_dialog();   return 0;
        case ID_BTN_FOLDER: open_apk_folder();   return 0;
        }
        break;

    case WM_CLOSE:
        DestroyWindow(hwnd);
        return 0;

    case WM_DESTROY:
        PostQuitMessage(0);
        return 0;
    }
    return DefWindowProcW(hwnd, msg, wp, lp);
}

int WINAPI WinMain(HINSTANCE inst, HINSTANCE prev, LPSTR cmd, int show)
{
    (void)prev; (void)cmd;
    InitializeCriticalSection(&g_logLock);

    GetModuleFileNameW(NULL, g_exeDir, MAX_PATH);
    wchar_t *slash = wcsrchr(g_exeDir, L'\\');
    if (slash) *slash = 0;

    WNDCLASSEXW wc;
    ZeroMemory(&wc, sizeof(wc));
    wc.cbSize = sizeof(wc);
    wc.lpfnWndProc = wnd_proc;
    wc.hInstance = inst;
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = (HBRUSH)(COLOR_BTNFACE + 1);
    wc.lpszClassName = L"OJMUSICLauncher";
    wc.hIcon = LoadIcon(NULL, IDI_APPLICATION);
    RegisterClassExW(&wc);

    HWND hwnd = CreateWindowExW(0, L"OJMUSICLauncher", L"OJ MUSIC  -  Made by Ochen Joshua",
                                WS_OVERLAPPEDWINDOW & ~WS_MAXIMIZEBOX,
                                CW_USEDEFAULT, CW_USEDEFAULT, 820, 560,
                                NULL, NULL, inst, NULL);
    if (!hwnd) return 1;

    ShowWindow(hwnd, show);
    UpdateWindow(hwnd);

    MSG msg;
    while (GetMessageW(&msg, NULL, 0, 0) > 0) {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }
    DeleteCriticalSection(&g_logLock);
    return 0;
}
