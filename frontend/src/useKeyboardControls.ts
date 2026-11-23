import { useEffect } from 'react';
import type { CommandBodyCommand as Command } from './api/generated/models';

const commands: Record<string, Command | undefined> = {
  ArrowLeft: 'LEFT',
  ArrowRight: 'RIGHT',
  ArrowUp: 'ROTATE',
  ' ': 'HARD_DROP',
};

export function useKeyboardControls(
  active: boolean,
  enabled: boolean,
  onCommand: (command: Command) => void,
  onPause: () => void,
) {
  useEffect(() => {
    if (!active) return;
    function onKeyDown(event: KeyboardEvent) {
      if (
        event.defaultPrevented ||
        event.isComposing ||
        event.ctrlKey ||
        event.altKey ||
        event.metaKey ||
        event.shiftKey
      )
        return;
      const target = event.target;
      if (target instanceof Element) {
        if (
          target.closest(
            'input, textarea, select, [contenteditable]:not([contenteditable="false"])',
          )
        )
          return;
        // Пробел сохраняет нативное нажатие сфокусированной кнопки или ссылки.
        if (event.key === ' ' && target.closest('button, a[href]')) return;
      }
      if (event.code === 'KeyP' || event.key === 'Escape') {
        event.preventDefault();
        if (enabled && !event.repeat) onPause();
        return;
      }
      const command = commands[event.key];
      if (!command) return;
      event.preventDefault();
      if (!enabled || (event.key === ' ' && event.repeat)) return;
      onCommand(command);
    }
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [active, enabled, onCommand, onPause]);
}
