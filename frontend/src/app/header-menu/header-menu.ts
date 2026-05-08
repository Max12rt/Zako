import { Component, ElementRef, HostListener, signal } from '@angular/core';

@Component({
  selector: 'app-header-menu',
  templateUrl: './header-menu.html',
  styleUrl: './header-menu.scss'
})
export class HeaderMenu {
  open = signal(false);

  constructor(private host: ElementRef<HTMLElement>) {}

  toggle() { this.open.update(v => !v); }
  close()  { this.open.set(false); }

  @HostListener('document:click', ['$event'])
  onDocClick(e: MouseEvent) {
    if (!this.open()) return;
    if (!this.host.nativeElement.contains(e.target as Node)) this.close();
  }

  @HostListener('document:keydown.escape')
  onEsc() { this.close(); }
}
