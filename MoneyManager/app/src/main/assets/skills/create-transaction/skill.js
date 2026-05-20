function processInput(inputJson) {
    var input = JSON.parse(inputJson);

    if (!input.amount || input.amount <= 0) {
        AndroidBridge.onError("Amount must be a positive number");
        return;
    }
    var validTypes = ["income","expense","savings","transfer","lend","borrow"];
    if (validTypes.indexOf(input.type) === -1) {
        AndroidBridge.onError("Invalid type '" + input.type + "'. Valid: " + validTypes.join(", "));
        return;
    }

    var date = input.date;
    if (typeof date === "string" && date.length > 0) {
        date = new Date(date).getTime();
        if (isNaN(date)) date = Date.now();
    } else if (typeof date !== "number") {
        date = Date.now();
    }

    var body = {
        amount: input.amount,
        type: input.type,
        description: input.description || "",
        date: date,
        accountName: input.accountName || null,
        categoryName: input.categoryName || null,
        note: input.note || "",
        peerName: input.peerName || null
    };

    fetch("http://localhost:18889/api/transactions", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    })
    .then(function(response) { return response.json(); })
    .then(function(result) {
        AndroidBridge.onResult(JSON.stringify(result));
    })
    .catch(function(err) {
        AndroidBridge.onError(err.message);
    });
}
