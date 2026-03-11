# Online Auction System: Actor-Based Microservice

This repository contains the implementation of an online auction system built using a microservice architecture and the Actor Model. The project leverages Akka (Scala) to handle asynchronous messaging, state persistence, and complex interactions between different entities in the system.

## 🏛️ System Architecture and Actors

The system relies on a set of specialized actors to manage the lifecycle of auctions, bids, and financial transactions.

- **eBay Actor**: The central orchestrator and intermediary of the system. It holds a map of all available auctions, tracking details like the item, highest bid, seller address, and active status.
- **Bank Actor**: Manages the financial sale of an auction. It verifies that bidders have sufficient funds, checks if both bidder and seller acknowledge the transaction, and processes the final amounts and refunds.
- **Seller Actor**: Responsible for creating and removing auctions. It tracks its sold auctions and the exact time of sale to process potential item returns.
- **Bidder Actor**: Places and withdraws bids on active auctions. It maintains information about the user's name, bank account, and the address to the eBay actor.
- **Auction Actor**: Represents the specific item being sold. It manages its own internal state, including the starting price, active duration, list of current bids, the current highest bid, and expiration time.

## ⚙️ Core Features

The system supports a full auction lifecycle:

- **Create & Remove Auctions**: Sellers can spawn an auction with a specific item, duration, and starting price, or manually cancel it.
- **Bidding System**: Bidders can request available auctions from eBay to place a random bid, or make a targeted bid on a specific item. Bids can also be withdrawn.
- **Automated Closing**: Auctions use internal timers. When the timer expires, the auction closes automatically and triggers the Bank to process the transaction and notify the winner and seller.
- **Returns & Refunds**: Bidders have a 5-second window after winning an auction to return the item and be refunded by the Bank.
- **Re-Auctioning**: Sellers can seamlessly re-list items that were returned or ended without a winner

## 📐 Design Patterns

This project implements several key actor-based and distributed system design patterns:

- **Event-Sourcing**: Used for state persistence. Every actor has a commandHandler for live behavior and an eventHandler to recover events and modify state from the event journal. State is managed immutably using state case classes (e.g., AuctionState).
- **Request-Response**: Implemented when an actor needs immediate feedback or results from another actor's computation. (e.g., a Bidder requesting available auctions from eBay, or the Bank verifying closing details).
- **Forward Flow Pattern**: Used when an actor needs to communicate with another actor whose exact address is unknown. The eBayActor frequently acts as the intermediary to forward these messages (e.g., a Bidder sending a bid to eBay, which forwards it to the correct Auction).

## 📄 Full Project Report

For a detailed breakdown of the actor interactions, state management, and architectural diagrams (including Sequence and State diagrams), please read the full assignment report:

👉 **[Read the Full Report (PDF)](./docs/Report.pdf)**
