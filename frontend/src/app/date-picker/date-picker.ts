import { Component, computed, ElementRef, EventEmitter, HostListener, Input, Output, signal } from '@angular/core';

const PL_MONTHS = [
  'Styczeń', 'Luty', 'Marzec', 'Kwiecień', 'Maj', 'Czerwiec',
  'Lipiec', 'Sierpień', 'Wrzesień', 'Październik', 'Listopad', 'Grudzień'
];

const PL_WEEKDAYS = ['pon', 'wt', 'śr', 'czw', 'pt', 'sob', 'ndz'];

interface DayCell {
  day: number;
  date: Date;
  inMonth: boolean;
  isToday: boolean;
}

@Component({
  selector: 'app-date-picker',
  templateUrl: './date-picker.html',
  styleUrl: './date-picker.scss'
})
export class DatePicker {
  @Input() value: Date = new Date();
  @Output() valueChange = new EventEmitter<Date>();

  open = signal(false);
  viewYear = signal(new Date().getFullYear());
  viewMonth = signal(new Date().getMonth());
  selected = signal(new Date());
  hour = signal(new Date().getHours());
  minute = signal(this.snapMinute(new Date().getMinutes()));

  weekdays = PL_WEEKDAYS;
  hours = Array.from({ length: 24 }, (_, i) => i);
  minutes = [0, 15, 30, 45];

  monthLabel = computed(() => `${PL_MONTHS[this.viewMonth()]} ${this.viewYear()}`);

  cells = computed<DayCell[]>(() => {
    const y = this.viewYear();
    const m = this.viewMonth();
    const first = new Date(y, m, 1);
    const startOffset = (first.getDay() + 6) % 7; // Monday-first
    const daysInPrev = new Date(y, m, 0).getDate();
    const daysInThis = new Date(y, m + 1, 0).getDate();
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const cells: DayCell[] = [];

    for (let i = startOffset - 1; i >= 0; i--) {
      const d = daysInPrev - i;
      const date = new Date(y, m - 1, d);
      cells.push({ day: d, date, inMonth: false, isToday: this.sameDay(date, today) });
    }
    for (let d = 1; d <= daysInThis; d++) {
      const date = new Date(y, m, d);
      cells.push({ day: d, date, inMonth: true, isToday: this.sameDay(date, today) });
    }
    while (cells.length % 7 !== 0 || cells.length < 42) {
      const d = cells.length - startOffset - daysInThis + 1;
      const date = new Date(y, m + 1, d);
      cells.push({ day: d, date, inMonth: false, isToday: this.sameDay(date, today) });
      if (cells.length >= 42) break;
    }
    return cells;
  });

  constructor(private host: ElementRef<HTMLElement>) {}

  ngOnInit() {
    this.syncFromValue(this.value);
  }

  ngOnChanges() {
    this.syncFromValue(this.value);
  }

  private syncFromValue(v: Date) {
    if (!v) return;
    this.viewYear.set(v.getFullYear());
    this.viewMonth.set(v.getMonth());
    this.selected.set(v);
    this.hour.set(v.getHours());
    this.minute.set(this.snapMinute(v.getMinutes()));
  }

  toggle() { this.open.update(v => !v); }
  closePanel() { this.open.set(false); }

  prevMonth() {
    const m = this.viewMonth() - 1;
    if (m < 0) { this.viewMonth.set(11); this.viewYear.update(y => y - 1); }
    else this.viewMonth.set(m);
  }

  nextMonth() {
    const m = this.viewMonth() + 1;
    if (m > 11) { this.viewMonth.set(0); this.viewYear.update(y => y + 1); }
    else this.viewMonth.set(m);
  }

  selectDay(cell: DayCell) {
    this.selected.set(new Date(cell.date.getFullYear(), cell.date.getMonth(), cell.date.getDate()));
    if (!cell.inMonth) {
      this.viewYear.set(cell.date.getFullYear());
      this.viewMonth.set(cell.date.getMonth());
    }
    this.emit();
  }

  selectHour(h: number)   { this.hour.set(h);   this.emit(); }
  selectMinute(m: number) { this.minute.set(m); this.emit(); }

  setNow() {
    const now = new Date();
    this.syncFromValue(now);
    this.emit();
  }

  isSelectedDay(cell: DayCell): boolean {
    return this.sameDay(cell.date, this.selected());
  }

  displayDate = computed(() => {
    const d = this.selected();
    return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}`;
  });
  displayTime = computed(() => `${pad(this.hour())}:${pad(this.minute())}`);

  private emit() {
    const d = this.selected();
    const out = new Date(d.getFullYear(), d.getMonth(), d.getDate(), this.hour(), this.minute());
    this.valueChange.emit(out);
  }

  private sameDay(a: Date, b: Date) {
    return a.getFullYear() === b.getFullYear()
        && a.getMonth() === b.getMonth()
        && a.getDate() === b.getDate();
  }

  private snapMinute(m: number) {
    return [0, 15, 30, 45].reduce((p, c) => Math.abs(c - m) < Math.abs(p - m) ? c : p, 0);
  }

  @HostListener('document:click', ['$event'])
  onDocClick(e: MouseEvent) {
    if (!this.open()) return;
    if (!this.host.nativeElement.contains(e.target as Node)) this.closePanel();
  }

  @HostListener('document:keydown.escape')
  onEsc() { this.closePanel(); }
}

function pad(n: number) { return n.toString().padStart(2, '0'); }
