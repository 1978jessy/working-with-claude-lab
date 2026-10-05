#!/usr/bin/env python3
"""
Generate the seed data for the Marlowe & Finch operations dashboard.

Writes three files, all derived from one deterministic run (seed 20260921):

  src/main/resources/db/migration/V2__seed.sql   the Flyway seed migration
  docs/data/deliveries-last-30-days.csv          the no-MCP fallback for Lab 4
  docs/data/ANSWER-KEY.md                        the true numbers, for mentors

Run it from the repository root:

  python3 tools/make_seed.py

The dashboard's "today" is fixed at 2026-09-21 (see the clock bean in the Java
code), so the data covers the 90 days before that date. Everything here is
fictional: the company, the carriers, the customers and the vendors.
"""
from __future__ import annotations

import csv
import random
from collections import Counter, defaultdict
from dataclasses import dataclass
from datetime import date, timedelta
from pathlib import Path

SEED = 20260921
TODAY = date(2026, 9, 21)
DAYS_OF_HISTORY = 90
FIRST_ORDER_DAY = TODAY - timedelta(days=DAYS_OF_HISTORY)  # 2026-06-23
LAST_ORDER_DAY = TODAY - timedelta(days=1)                 # 2026-09-20

ROOT = Path(__file__).resolve().parent.parent
SQL_OUT = ROOT / "src/main/resources/db/migration/V2__seed.sql"
CSV_OUT = ROOT / "docs/data/deliveries-last-30-days.csv"
KEY_OUT = ROOT / "docs/data/ANSWER-KEY.md"

# ---------------------------------------------------------------------------
# Reference data
# ---------------------------------------------------------------------------

# (id, name, code, share of shipments, base on-time rate)
CARRIERS = [
    (1, "Kessler Logistics", "KES", 0.40, None),   # rate slips over the summer, see kessler_on_time_rate
    (2, "Northwind Freight", "NWF", 0.25, 0.97),
    (3, "Harbour Express", "HBX", 0.20, 0.98),
    (4, "Redwood Couriers", "RWC", 0.15, 0.96),
]

KESSLER_RATE_START = 0.93   # around 1 July
KESSLER_RATE_END = 0.90     # by mid September
KESSLER_SLIP_FROM = date(2026, 7, 1)
KESSLER_SLIP_TO = date(2026, 9, 15)

REGIONS = ["London", "South East", "South West", "Midlands", "North", "Scotland", "Wales"]

CUSTOMERS = [
    "Bramble Lane Coffee", "The Copper Kettle", "Harrow Roasters", "Pennyfarthing Cafe",
    "Ashdown Bakehouse", "Tidewater Espresso", "Marigold and Sons", "Fenwick Street Coffee",
    "Old Mill Roastery", "Larkspur Tea Rooms", "Blackwell Coffee Co", "Quayside Beans",
    "Holloway Grind", "Sable and Oak", "Riverbank Kiosk", "Cobblers Yard Cafe",
    "Northbridge Roasters", "Peartree Deli", "Lantern House", "Greyfriars Coffee",
    "The Brass Tap", "Elderflower Kitchen", "Kingsway Canteen", "Silverbirch Cafe",
    "Bexley Hill Bakery", "Wren and Willow", "Foundry Coffee", "Saltmarsh Espresso",
    "Corner Post Coffee", "Hatchery Roasters", "Meadowsweet Cafe", "Stonebridge Beans",
    "Amberley Roasting", "Coalport Coffee House", "Nightjar Espresso", "Ivy Court Cafe",
    "Beacon Hill Roasters", "Redgate Coffee", "Tallow and Bean", "Westgate Canteen",
]

# (name, unit price)
PRODUCTS = [
    ("Filter papers, 500 pack", 18.50),
    ("Group head gasket kit", 24.00),
    ("Milk jug 600 ml", 29.00),
    ("Tamper 58 mm", 42.00),
    ("Descaling solution, 6 x 1 l", 54.00),
    ("Portafilter, bottomless", 68.00),
    ("Precision basket set", 72.00),
    ("Barista scale", 89.00),
    ("Hand grinder", 145.00),
    ("Water filter cartridge, 4 pack", 160.00),
    ("Batch brewer 2.5 l", 420.00),
    ("Shop grinder 64 mm", 895.00),
    ("Shop grinder 80 mm", 1480.00),
    ("Espresso machine, single group", 2650.00),
    ("Espresso machine, two group", 4800.00),
]

TICKET_CATEGORIES = ["Delivery delay", "Damaged on arrival", "Billing question", "Missing parts", "Warranty claim"]
TICKET_COUNTS = {
    "Delivery delay": 180,
    "Damaged on arrival": 120,
    "Billing question": 110,
    "Missing parts": 100,
    "Warranty claim": 90,
}
# Share of the two delivery-related categories that land on Kessler deliveries.
KESSLER_SHARE = {"Delivery delay": 0.75, "Damaged on arrival": 0.65}

VENDORS = [
    # id, name, category, annual spend, contract end, notice days, owner
    (1, "Kessler Logistics", "Carrier", 412000, date(2026, 11, 30), 90, "Priya Nandakumar"),
    (2, "BrightLeaf Packaging", "Packaging", 86500, date(2027, 3, 31), 60, "Tom Aldridge"),
    (3, "Volta Parts GmbH", "Spare parts", 238000, date(2026, 10, 15), 30, "Hanna Lindqvist"),
    (4, "Northgate Insurance", "Insurance", 54200, date(2027, 1, 1), 90, "Marcus Bell"),
    (5, "HelpSpark", "Support desk software", 31800, date(2026, 12, 31), 30, "Aisha Rahman"),
    (6, "Pinecrest Staffing", "Temporary staff", 147000, date(2027, 6, 30), 60, "Tom Aldridge"),
    (7, "Lumen Creative", "Marketing agency", 62000, date(2026, 10, 31), 45, "Sofia Marchetti"),
    (8, "CloudCart", "E-commerce platform", 96000, date(2027, 4, 30), 90, "Marcus Bell"),
]


# ---------------------------------------------------------------------------
# Rows
# ---------------------------------------------------------------------------

@dataclass
class Order:
    id: int
    order_ref: str
    customer_name: str
    region: str
    ordered_at: date
    amount: float
    status: str  # processing, shipped, cancelled


@dataclass
class Delivery:
    id: int
    order_id: int
    carrier_id: int
    shipped_date: date
    promised_date: date
    delivered_date: date | None
    days_late: int | None


@dataclass
class Ticket:
    id: int
    order_id: int
    category: str
    priority: str
    status: str  # open, closed
    opened_at: date
    closed_at: date | None


def kessler_on_time_rate(day: date) -> float:
    span = (KESSLER_SLIP_TO - KESSLER_SLIP_FROM).days
    frac = (day - KESSLER_SLIP_FROM).days / span
    frac = max(0.0, min(1.1, frac))
    return KESSLER_RATE_START - (KESSLER_RATE_START - KESSLER_RATE_END) * frac


def pick_carrier(rng: random.Random) -> int:
    r = rng.random()
    acc = 0.0
    for cid, _, _, share, _ in CARRIERS:
        acc += share
        if r < acc:
            return cid
    return CARRIERS[-1][0]


def on_time_probability(carrier_id: int, day: date) -> float:
    if carrier_id == 1:
        return kessler_on_time_rate(day)
    for cid, _, _, _, rate in CARRIERS:
        if cid == carrier_id:
            return rate
    raise ValueError(carrier_id)


def make_orders(rng: random.Random) -> list[Order]:
    orders: list[Order] = []
    day = FIRST_ORDER_DAY
    next_id = 1
    while day <= LAST_ORDER_DAY:
        n = rng.randint(22, 32) if day.weekday() < 5 else rng.randint(6, 14)
        for _ in range(n):
            lines = rng.choices([1, 2, 3], weights=[60, 30, 10])[0]
            amount = 0.0
            for _ in range(lines):
                _, price = rng.choices(PRODUCTS, weights=[14, 10, 9, 8, 8, 6, 6, 6, 5, 5, 4, 3, 2, 1, 1])[0]
                amount += price * rng.randint(1, 3)
            if day >= TODAY - timedelta(days=2):
                status = "processing"
            elif rng.random() < 0.03:
                status = "cancelled"
            else:
                status = "shipped"
            orders.append(Order(
                id=next_id,
                order_ref=f"MF-{next_id:05d}",
                customer_name=rng.choice(CUSTOMERS),
                region=rng.choice(REGIONS),
                ordered_at=day,
                amount=round(amount, 2),
                status=status,
            ))
            next_id += 1
        day += timedelta(days=1)
    return orders


def make_deliveries(rng: random.Random, orders: list[Order]) -> list[Delivery]:
    deliveries: list[Delivery] = []
    next_id = 1
    for order in orders:
        if order.status != "shipped":
            continue
        carrier_id = pick_carrier(rng)
        shipped = order.ordered_at + timedelta(days=rng.randint(1, 2))
        promised = shipped + timedelta(days=rng.randint(2, 4))
        if rng.random() < on_time_probability(carrier_id, shipped):
            delivered = promised - timedelta(days=rng.choices([0, 1], weights=[70, 30])[0])
            days_late = 0
        else:
            days_late = rng.choices([1, 2, 3, 4, 5, 6, 7], weights=[45, 25, 14, 7, 5, 2, 2])[0]
            delivered = promised + timedelta(days=days_late)
        if delivered > LAST_ORDER_DAY:
            delivered = None
            days_late = None
        deliveries.append(Delivery(next_id, order.id, carrier_id, shipped, promised, delivered, days_late))
        next_id += 1
    return deliveries


def make_tickets(rng: random.Random, orders: list[Order], deliveries: list[Delivery]) -> list[Ticket]:
    by_order = {o.id: o for o in orders}
    delivered = [d for d in deliveries if d.delivered_date is not None]
    kessler = [d for d in delivered if d.carrier_id == 1]
    kessler_late = [d for d in kessler if d.days_late]
    others = [d for d in delivered if d.carrier_id != 1]
    others_late = [d for d in others if d.days_late]
    shipped_orders = [o for o in orders if o.status == "shipped"]

    tickets: list[Ticket] = []
    next_id = 1
    for category in TICKET_CATEGORIES:
        for _ in range(TICKET_COUNTS[category]):
            if category in KESSLER_SHARE:
                on_kessler = rng.random() < KESSLER_SHARE[category]
                if category == "Delivery delay":
                    pool = kessler_late if on_kessler else others_late
                else:
                    pool = kessler if on_kessler else others
                d = rng.choice(pool)
                order_id = d.order_id
                opened = d.delivered_date + timedelta(days=rng.randint(0, 3))
                priority = rng.choices(["low", "normal", "high"], weights=[10, 55, 35])[0]
            else:
                o = rng.choice(shipped_orders)
                order_id = o.id
                opened = o.ordered_at + timedelta(days=rng.randint(2, 12))
                priority = rng.choices(["low", "normal", "high"], weights=[35, 50, 15])[0]
            if opened > LAST_ORDER_DAY:
                opened = LAST_ORDER_DAY - timedelta(days=rng.randint(0, 2))
            age = (TODAY - opened).days
            if age > 14:
                closed = rng.random() < 0.9
            elif age > 5:
                closed = rng.random() < 0.55
            else:
                closed = rng.random() < 0.2
            closed_at = None
            if closed:
                closed_at = opened + timedelta(days=rng.randint(1, 8))
                if closed_at > LAST_ORDER_DAY:
                    closed_at = None
                    closed = False
            tickets.append(Ticket(
                id=next_id,
                order_id=order_id,
                category=category,
                priority=priority,
                status="closed" if closed else "open",
                opened_at=opened,
                closed_at=closed_at,
            ))
            next_id += 1
    tickets.sort(key=lambda t: (t.opened_at, t.id))
    for i, t in enumerate(tickets, start=1):
        t.id = i
    return tickets


# ---------------------------------------------------------------------------
# SQL
# ---------------------------------------------------------------------------

def sql_str(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def sql_value(value) -> str:
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "TRUE" if value else "FALSE"
    if isinstance(value, (int, float)):
        return f"{value:.2f}" if isinstance(value, float) else str(value)
    if isinstance(value, date):
        return f"DATE '{value.isoformat()}'"
    return sql_str(str(value))


def insert_block(table: str, columns: list[str], rows: list[list], chunk: int = 250) -> str:
    out = []
    for start in range(0, len(rows), chunk):
        part = rows[start:start + chunk]
        values = ",\n".join("  (" + ", ".join(sql_value(v) for v in row) + ")" for row in part)
        out.append(f"INSERT INTO {table} ({', '.join(columns)}) VALUES\n{values};\n")
    return "\n".join(out)


def write_sql(orders, deliveries, tickets) -> None:
    parts = [
        "-- Seed data for the Marlowe & Finch operations dashboard.\n"
        f"-- Generated by tools/make_seed.py (seed {SEED}). Do not edit by hand; re-run the script.\n"
        f"-- Covers the {DAYS_OF_HISTORY} days before {TODAY.isoformat()}, which the app treats as today.\n",
        insert_block("carriers", ["id", "name", "code"], [[c[0], c[1], c[2]] for c in CARRIERS]),
        insert_block("vendors",
                     ["id", "name", "category", "annual_spend", "contract_end", "notice_days", "owner"],
                     [[v[0], v[1], v[2], float(v[3]), v[4], v[5], v[6]] for v in VENDORS]),
        insert_block("orders",
                     ["id", "order_ref", "customer_name", "region", "ordered_at", "amount", "status"],
                     [[o.id, o.order_ref, o.customer_name, o.region, o.ordered_at, o.amount, o.status] for o in orders]),
        insert_block("deliveries",
                     ["id", "order_id", "carrier_id", "shipped_date", "promised_date", "delivered_date", "days_late"],
                     [[d.id, d.order_id, d.carrier_id, d.shipped_date, d.promised_date, d.delivered_date, d.days_late]
                      for d in deliveries]),
        insert_block("tickets",
                     ["id", "order_id", "category", "priority", "status", "opened_at", "closed_at"],
                     [[t.id, t.order_id, t.category, t.priority, t.status, t.opened_at, t.closed_at] for t in tickets]),
    ]
    SQL_OUT.write_text("\n".join(parts), encoding="utf-8")


# ---------------------------------------------------------------------------
# CSV and answer key
# ---------------------------------------------------------------------------

def in_range(day: date | None, start: date, end: date) -> bool:
    return day is not None and start <= day <= end


def write_csv(orders, deliveries) -> int:
    by_order = {o.id: o for o in orders}
    names = {c[0]: c[1] for c in CARRIERS}
    start = TODAY - timedelta(days=30)
    rows = [d for d in deliveries if in_range(d.delivered_date, start, TODAY)]
    rows.sort(key=lambda d: (d.delivered_date, d.id))
    with CSV_OUT.open("w", newline="", encoding="utf-8") as fh:
        w = csv.writer(fh)
        w.writerow(["order_id", "carrier", "promised_date", "delivered_date", "days_late"])
        for d in rows:
            w.writerow([by_order[d.order_id].order_ref, names[d.carrier_id],
                        d.promised_date.isoformat(), d.delivered_date.isoformat(), d.days_late])
    return len(rows)


def carrier_stats(deliveries, start, end):
    names = {c[0]: c[1] for c in CARRIERS}
    stats = {name: {"delivered": 0, "on_time": 0, "late": 0, "days_late": 0} for name in names.values()}
    for d in deliveries:
        if not in_range(d.delivered_date, start, end):
            continue
        s = stats[names[d.carrier_id]]
        s["delivered"] += 1
        if d.days_late == 0:
            s["on_time"] += 1
        else:
            s["late"] += 1
            s["days_late"] += d.days_late
    return stats


def rate(s) -> str:
    return "n/a" if s["delivered"] == 0 else f"{100.0 * s['on_time'] / s['delivered']:.1f}%"


def write_answer_key(orders, deliveries, tickets, csv_rows: int) -> dict:
    d7_from, d30_from = TODAY - timedelta(days=7), TODAY - timedelta(days=30)
    s7 = carrier_stats(deliveries, d7_from, TODAY)
    s30 = carrier_stats(deliveries, d30_from, TODAY)
    worst_week = max(s7.items(), key=lambda kv: (kv[1]["late"], kv[0]))

    open_all = Counter(t.category for t in tickets if t.status == "open")
    open_30 = Counter(t.category for t in tickets if t.status == "open" and in_range(t.opened_at, d30_from, TODAY))
    total_30 = Counter(t.category for t in tickets if in_range(t.opened_at, d30_from, TODAY))
    kessler_orders = {d.order_id for d in deliveries if d.carrier_id == 1}
    on_kessler = Counter(t.category for t in tickets if t.order_id in kessler_orders)
    per_cat = Counter(t.category for t in tickets)

    revenue_by_month = defaultdict(float)
    orders_by_month = Counter()
    for o in orders:
        key = o.ordered_at.strftime("%Y-%m")
        orders_by_month[key] += 1
        if o.status != "cancelled":
            revenue_by_month[key] += o.amount

    kpi_orders = sum(1 for o in orders if in_range(o.ordered_at, d30_from, TODAY))
    kpi_revenue = sum(o.amount for o in orders if in_range(o.ordered_at, d30_from, TODAY) and o.status != "cancelled")
    kpi_delivered = sum(s["delivered"] for s in s30.values())
    kpi_on_time = sum(s["on_time"] for s in s30.values())
    kpi_open = sum(open_30.values())

    monthly_kessler = {}
    for month in ("2026-07", "2026-08", "2026-09"):
        first = date(int(month[:4]), int(month[5:]), 1)
        last = (first.replace(day=28) + timedelta(days=4)).replace(day=1) - timedelta(days=1)
        monthly_kessler[month] = carrier_stats(deliveries, first, min(last, TODAY))["Kessler Logistics"]

    lines = []
    lines.append("# Answer key: the seed data behind the dashboard\n")
    lines.append(f"Generated by `tools/make_seed.py` with seed {SEED}. Do not hand this out before Part 4.\n")
    lines.append("The app's clock is fixed at **2026-09-21**. \"Last 7 days\" means "
                 f"{d7_from.isoformat()} to {TODAY.isoformat()} inclusive and \"last 30 days\" means "
                 f"{d30_from.isoformat()} to {TODAY.isoformat()}, which is exactly what the `/api` "
                 "endpoints and the dashboard presets use. Delivery figures are by the date the parcel "
                 "arrived (`delivered_date`); orders and tickets by the date they were placed or opened.\n")
    lines.append("## Totals\n")
    lines.append(f"- Orders: **{len(orders)}** ({FIRST_ORDER_DAY.isoformat()} to {LAST_ORDER_DAY.isoformat()}), "
                 f"of which {sum(1 for o in orders if o.status == 'shipped')} shipped, "
                 f"{sum(1 for o in orders if o.status == 'processing')} still processing, "
                 f"{sum(1 for o in orders if o.status == 'cancelled')} cancelled")
    lines.append(f"- Deliveries: **{len(deliveries)}** (one per shipped order), "
                 f"{sum(1 for d in deliveries if d.delivered_date is None)} still in transit")
    lines.append(f"- Tickets: **{len(tickets)}** in {len(TICKET_CATEGORIES)} categories, "
                 f"{sum(1 for t in tickets if t.status == 'open')} open")
    lines.append(f"- Carriers: {len(CARRIERS)}; vendors: {len(VENDORS)}\n")

    lines.append("## The Lab 4 question: which carrier was late most often last week?\n")
    lines.append(f"**{worst_week[0]}**, with **{worst_week[1]['late']}** late deliveries between "
                 f"{d7_from.isoformat()} and {TODAY.isoformat()} "
                 f"(total {worst_week[1]['days_late']} days late across them).\n")
    lines.append("| Carrier | Delivered | On time | Late | Days late (sum) | On-time rate |")
    lines.append("|---|---:|---:|---:|---:|---:|")
    for name, s in sorted(s7.items(), key=lambda kv: -kv[1]["late"]):
        lines.append(f"| {name} | {s['delivered']} | {s['on_time']} | {s['late']} | {s['days_late']} | {rate(s)} |")
    lines.append("")
    lines.append(f"The CSV fallback `docs/data/deliveries-last-30-days.csv` has {csv_rows} rows "
                 f"(every delivery that arrived between {d30_from.isoformat()} and {TODAY.isoformat()}, "
                 "on time or not). Filtering it to `delivered_date >= "
                 f"{d7_from.isoformat()}` and `days_late > 0` gives the same table.\n")

    lines.append("## On-time delivery rate per carrier, last 30 days\n")
    lines.append("| Carrier | Delivered | On time | Late | On-time rate |")
    lines.append("|---|---:|---:|---:|---:|")
    for name, s in sorted(s30.items(), key=lambda kv: kv[0]):
        lines.append(f"| {name} | {s['delivered']} | {s['on_time']} | {s['late']} | {rate(s)} |")
    lines.append("")
    lines.append("Kessler Logistics by month (the slip the dashboard is meant to surface):\n")
    lines.append("| Month | Delivered | On-time rate |")
    lines.append("|---|---:|---:|")
    for month, s in monthly_kessler.items():
        lines.append(f"| {month} | {s['delivered']} | {rate(s)} |")
    lines.append("")

    lines.append("## Open tickets by category\n")
    lines.append("| Category | Open now (all time) | Opened last 30 days | Of which still open | On Kessler deliveries (all time) |")
    lines.append("|---|---:|---:|---:|---:|")
    for cat in TICKET_CATEGORIES:
        lines.append(f"| {cat} | {open_all[cat]} | {total_30[cat]} | {open_30[cat]} | {on_kessler[cat]} of {per_cat[cat]} |")
    lines.append("")

    lines.append("## Revenue by month (cancelled orders excluded)\n")
    lines.append("| Month | Orders placed | Revenue |")
    lines.append("|---|---:|---:|")
    for month in sorted(orders_by_month):
        lines.append(f"| {month} | {orders_by_month[month]} | £{revenue_by_month[month]:,.2f} |")
    lines.append("")

    lines.append("## KPI tiles with the default range (last 30 days)\n")
    lines.append(f"- On-time delivery rate: **{100.0 * kpi_on_time / kpi_delivered:.1f}%** ({kpi_on_time} of {kpi_delivered} deliveries)")
    lines.append(f"- Open tickets: **{kpi_open}** (opened in the range and still open)")
    lines.append(f"- Revenue: **£{kpi_revenue:,.2f}**")
    lines.append(f"- Orders: **{kpi_orders}**\n")

    lines.append("## Vendors: contract renewals inside their notice window on 2026-09-21\n")
    lines.append("| Vendor | Contract end | Days until end | Notice days | Highlighted |")
    lines.append("|---|---|---:|---:|---|")
    for _, name, _, _, end, notice, _ in sorted(VENDORS, key=lambda v: v[4]):
        days = (end - TODAY).days
        lines.append(f"| {name} | {end.isoformat()} | {days} | {notice} | {'yes' if days <= notice else 'no'} |")
    lines.append("")
    KEY_OUT.write_text("\n".join(lines), encoding="utf-8")

    return {
        "orders": len(orders), "deliveries": len(deliveries), "tickets": len(tickets),
        "worst_week": worst_week, "s7": s7, "s30": s30, "open_30": open_30, "total_30": total_30,
        "kpi_orders": kpi_orders, "kpi_revenue": kpi_revenue, "kpi_delivered": kpi_delivered,
        "kpi_on_time": kpi_on_time, "kpi_open": kpi_open, "csv_rows": csv_rows,
        "revenue_by_month": dict(revenue_by_month),
    }


def main() -> None:
    rng = random.Random(SEED)
    orders = make_orders(rng)
    deliveries = make_deliveries(rng, orders)
    tickets = make_tickets(rng, orders, deliveries)
    write_sql(orders, deliveries, tickets)
    csv_rows = write_csv(orders, deliveries)
    summary = write_answer_key(orders, deliveries, tickets, csv_rows)
    print(f"orders={summary['orders']} deliveries={summary['deliveries']} tickets={summary['tickets']} csv_rows={csv_rows}")
    print(f"worst carrier last week: {summary['worst_week'][0]} with {summary['worst_week'][1]['late']} late deliveries")
    for name, s in sorted(summary["s30"].items()):
        print(f"  30d {name}: {s['on_time']}/{s['delivered']} = {rate(s)}")
    print(f"kpis 30d: orders={summary['kpi_orders']} revenue={summary['kpi_revenue']:.2f} "
          f"on_time={summary['kpi_on_time']}/{summary['kpi_delivered']} open_tickets={summary['kpi_open']}")
    print(f"wrote {SQL_OUT.relative_to(ROOT)}, {CSV_OUT.relative_to(ROOT)}, {KEY_OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
