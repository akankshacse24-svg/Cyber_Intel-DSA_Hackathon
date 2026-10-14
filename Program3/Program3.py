
import csv
import networkx as nx
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation


# Step 1: Find minimum cost and selected route
def find_min_cost(N, flights, K, C):
    max_flights = K + 1
    INF = float("inf")

    dp = [
        [[INF] * (C + 1) for _ in range(N + 1)]
        for _ in range(max_flights + 1)
    ]

    dp[0][1][0] = 0
    parent = {}

    for f in range(1, max_flights + 1):
        for index, (u, v, price) in enumerate(flights):
            for used in range(C + 1):
                if dp[f - 1][u][used] == INF:
                    continue

                # Pay full price
                cost = dp[f - 1][u][used] + price

                if cost < dp[f][v][used]:
                    dp[f][v][used] = cost
                    parent[(f, v, used)] = (
                        u, used, False, index
                    )

                # Use coupon
                if used < C:
                    cost = dp[f - 1][u][used] + price // 2

                    if cost < dp[f][v][used + 1]:
                        dp[f][v][used + 1] = cost
                        parent[(f, v, used + 1)] = (
                            u, used, True, index
                        )

    best_cost = INF
    best_state = None

    for f in range(max_flights + 1):
        for used in range(C + 1):
            if dp[f][N][used] < best_cost:
                best_cost = dp[f][N][used]
                best_state = (f, N, used)

    if best_cost == INF:
        return -1, [], []

    route = []
    state = best_state

    while state[0] > 0:
        previous_city, previous_used, coupon, index = parent[state]
        u, v, price = flights[index]

        route.append({
            "index": index,
            "from": u,
            "to": v,
            "original_price": price,
            "coupon_used": coupon,
            "final_price": price // 2 if coupon else price
        })

        f, _, _ = state
        state = (f - 1, previous_city, previous_used)

    route.reverse()
    cities = [1] + [flight["to"] for flight in route]

    return best_cost, cities, route


# Step 2: Read CSV input
def read_flights_csv(filename):
    flights = []

    with open(filename, "r", newline="") as file:
        reader = csv.DictReader(file)
        required = {"from", "to", "price"}

        if not reader.fieldnames or not required.issubset(
            set(reader.fieldnames)
        ):
            raise ValueError(
                "CSV headings must be: from,to,price"
            )

        for row in reader:
            u = int(row["from"])
            v = int(row["to"])
            price = int(row["price"])

            if u < 1 or v < 1 or price < 0:
                raise ValueError(
                    "City numbers must be positive and prices non-negative."
                )

            flights.append((u, v, price))

    return flights


# Step 3: Write results to CSV
def write_result_csv(filename, flights, route):
    selected = {flight["index"]: flight for flight in route}

    with open(filename, "w", newline="") as file:
        fields = [
            "from", "to", "price",
            "selected", "coupon_used", "final_price"
        ]

        writer = csv.DictWriter(file, fieldnames=fields)
        writer.writeheader()

        for index, (u, v, price) in enumerate(flights):
            flight = selected.get(index)

            writer.writerow({
                "from": u,
                "to": v,
                "price": price,
                "selected": "x" if flight else "",
                "coupon_used": (
                    "Yes" if flight and flight["coupon_used"] else "No"
                ),
                "final_price": (
                    flight["final_price"] if flight else price
                )
            })


# Step 4 and 5: Draw and automatically animate the route
def visualize_graph(N, flights, route, cost):
    G = nx.MultiDiGraph()
    G.add_nodes_from(range(1, N + 1))

    for index, (u, v, price) in enumerate(flights):
        G.add_edge(u, v, key=index, weight=price)

    pos = nx.spring_layout(G, seed=42)

    fig, ax = plt.subplots(figsize=(11, 7))
    ax.set_axis_off()

    route_cities = set()
    for flight in route:
        route_cities.add(flight["from"])
        route_cities.add(flight["to"])

    node_colors = []
    for city in G.nodes():
        if city == 1:
            node_colors.append("green")
        elif city == N:
            node_colors.append("red")
        elif city in route_cities:
            node_colors.append("orange")
        else:
            node_colors.append("lightgray")

    # Draw all flights
    nx.draw_networkx_edges(
        G, pos, ax=ax,
        edge_color="lightgray",
        arrows=True, arrowsize=18,
        connectionstyle="arc3,rad=0.10"
    )

    nx.draw_networkx_nodes(
        G, pos, ax=ax,
        node_color=node_colors,
        node_size=1100
    )

    nx.draw_networkx_labels(
        G, pos, ax=ax,
        font_size=12, font_weight="bold"
    )

    edge_labels = {
        (u, v, index): f"Rs.{price}"
        for index, (u, v, price) in enumerate(flights)
    }

    nx.draw_networkx_edge_labels(
        G, pos, ax=ax,
        edge_labels=edge_labels,
        font_size=8
    )

    # Animation information
    status = ax.text(
        0.02, 0.97, "",
        transform=ax.transAxes,
        fontsize=12,
        verticalalignment="top",
        bbox=dict(facecolor="white", alpha=0.9)
    )

    if cost == -1:
        ax.set_title("No valid route found")
        status.set_text("No route reaches the destination within the limits.")
        plt.tight_layout()
        plt.show()
        return

    ax.set_title("Budget Traveller - Automatic Route Animation")

    # Store drawn route arrows so they remain visible
    animated_edges = []

    def update(frame):
        # Remove the previous animation's arrows
        for edge_artist in animated_edges:
            edge_artist.remove()
        animated_edges.clear()

        running_cost = 0

        # Draw each flight up to the current animation frame
        for i in range(frame):
            flight = route[i]
            u = flight["from"]
            v = flight["to"]
            index = flight["index"]

            color = "gold" if flight["coupon_used"] else "blue"
            width = 4 if flight["coupon_used"] else 3

            artist = nx.draw_networkx_edges(
                G, pos, ax=ax,
                edgelist=[(u, v, index)],
                edge_color=color,
                width=width,
                arrows=True,
                arrowsize=25,
                connectionstyle="arc3,rad=0.10"
            )

            if artist is not None:
                if isinstance(artist, list):
                    animated_edges.extend(artist)
                else:
                    animated_edges.append(artist)

            running_cost += flight["final_price"]

        if frame == 0:
            status.set_text("Starting journey... Running cost: Rs.0")
        else:
            last_flight = route[frame - 1]

            if last_flight["coupon_used"]:
                detail = (
                    f'Coupon used! Rs.{last_flight["original_price"]} '
                    f'-> Rs.{last_flight["final_price"]}'
                )
            else:
                detail = (
                    f'Flight {last_flight["from"]} -> '
                    f'{last_flight["to"]}'
                )

            status.set_text(
                f"{detail}\n"
                f"Flights completed: {frame}/{len(route)}\n"
                f"Running cost: Rs.{running_cost}"
            )

        if frame == len(route):
            status.set_text(
                f"Journey completed!\n"
                f"Minimum cost: Rs.{cost}"
            )

        return [status, *animated_edges]

    animation = FuncAnimation(
        fig,
        update,
        frames=range(len(route) + 1),
        interval=1200,
        repeat=False,
        blit=False,
        cache_frame_data=False
    )

    # Keep a reference to the animation alive
    fig._route_animation = animation

    plt.tight_layout()
    plt.show()


# Main program
def main():
    try:
        N = int(input("Enter number of cities (N): "))
        K = int(input("Enter maximum stops (K): "))
        C = int(input("Enter number of coupons (C): "))

        if N < 1 or K < 0 or C < 0:
            raise ValueError(
                "N must be positive; K and C cannot be negative."
            )

        flights = read_flights_csv("flights.csv")

        for u, v, price in flights:
            if u > N or v > N:
                raise ValueError(
                    "A flight contains a city number greater than N."
                )

        cost, cities, route = find_min_cost(N, flights, K, C)

        print("\nMinimum cost:", cost)

        if cost != -1:
            print("Route:", " -> ".join(map(str, cities)))
            print("\nSelected flights:")

            for flight in route:
                if flight["coupon_used"]:
                    print(
                        f'{flight["from"]} -> {flight["to"]}: '
                        f'{flight["original_price"]} -> '
                        f'{flight["final_price"]} (Coupon used)'
                    )
                else:
                    print(
                        f'{flight["from"]} -> {flight["to"]}: '
                        f'{flight["original_price"]} (No coupon)'
                    )

        write_result_csv("result.csv", flights, route)
        print("\nCSV output saved to result.csv")

        visualize_graph(N, flights, route, cost)

    except (ValueError, KeyError, FileNotFoundError) as error:
        print("Error:", error)


if __name__ == "__main__":
    main()